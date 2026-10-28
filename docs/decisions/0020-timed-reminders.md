# 0020 — Timed reminders — Event / Task / Habit

- **Status:** Accepted
- **Date:** 2026-09-08
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Extends the Phase 6 notification stack from "daily digest at a fixed hour" to "notify at the item's own time", in one pass across three domains right after the integration-test work. `EventReminderJob` (Calendar, 2026-09-04) already had the per-minute-poll shape; this generalised it to Task and Habit and settled the per-item default for Event.

Decisions, held through implementation:

- **Two distinct notification intents, kept separate.** "It's time to do X" (a reminder at/before the scheduled moment — per-minute poll, per-item time) vs. "you're about to lose something" (a daily digest at a sensible hour — `TaskDueReminderJob` 18:00, `HabitAtRiskJob` 20:00). The digests were **not** replaced. A habit scheduled at 09:00 that's still undone by 20:00 gets *both* the 09:00 "es hora" nudge and the 20:00 "racha en riesgo" alert — different messages, both wanted; `HabitAtRiskJob` and `findHabitsAtRisk()` were not touched.
- **"Before" vs "at" is domain-specific.** Event reminders fire *before* `startAt` (a notification at the exact start is already too late to prepare) — default 10 min. Task and Habit fire *at* the scheduled time, no lead time — a global or per-item lead time was considered and declined (YAGNI, single user, no evidence of a need for variable anticipation).
- **Dedup marker differs by recurrence.** Event/Task are one-shot → a `reminder_sent` boolean, reset when the schedule changes. Habit recurs daily → a `last_reminded_on` `LocalDate` (fire only if `!= today`), which self-resets at midnight with no reset job — a boolean would have needed one.
- **One job per domain, not a unified `ReminderJob`.** Three near-identical `@Scheduled(cron = "0 * * * * *", zone = "${align.timezone}")` polls (`EventReminderJob`, `TaskReminderJob`, `HabitReminderJob`), each asking its own domain service for candidates and composing the copy in the job (same domain-returns-candidates / job-composes-copy split as Phase 6). Consolidation into one job is a later refactor if it gets unwieldy; at single-user scale each poll is one query usually returning zero rows.

## Event — default reminder (`EventServiceImpl.applyReminder`)

`reminderMinutesBefore == null` now means "10 minutes before" instead of "no reminder": `applyReminder` sets `reminderAt = startAt.minusMinutes(DEFAULT_REMINDER_MINUTES_BEFORE)` (constant, `= 10`) and resets `reminderSent`, instead of the old branch that left `reminderAt` null (the one with the missing-`return` NPE bug). Every event now has a `reminderAt` and notifies. **Consequence accepted:** there's no way to express "this event, no reminder at all" — add a sentinel (`reminderMinutesBefore = -1`) or a separate boolean only if that need shows up. `EventReminderJob` unchanged.

## Task — timed reminders + `dueDate` now mandatory

**Timed reminders:**

- `Task.reminderSent` (`boolean`, `@Column(nullable = false)`), migration `V16__add_reminder_sent_to_tasks.sql` (`ADD COLUMN ... BOOLEAN NOT NULL DEFAULT FALSE`). Not in `TaskResponse` — internal, same as Event.
- `TaskSpecifications.dueForReminder(eligibleStatuses, today, now)` — `status IN (PENDING, IN_PROGRESS) AND dueDate = today AND dueTime IS NOT NULL AND dueTime <= now AND reminderSent = false`. `<=` (not `<` like `expirable`) so it fires *at* the minute. `TaskService.findTasksDueForReminder()` / `markReminderSent(UUID)` (system sweep, `findById`, no `User`), same shape as Event's.
- `TaskReminderJob` (`scheduler`, per minute) — "Tarea" / "«X» vence a las HH:mm" / `/tasks`, `markReminderSent` per item.
- `updateTask` re-arms: `reArmReminderIfDueChanged` captures `dueDate`/`dueTime` before `mapper.updateEntity`, sets `reminderSent = false` only if either changed — a blind reset would re-fire a past-time reminder on an unrelated edit (e.g. title). Named differently from Event's `applyReminder` on purpose: it doesn't compute anything, just re-arms.
- `TaskDueReminderJob` (18:00) now handles **only untimed tasks**: `findTasksDueToday` → `repository.findAllByDueDateAndDueTimeIsNullAndStatusNot(today, COMPLETED)`. Timed tasks got their push at their time; without this filter they'd also get a second one at 18:00. The old derived method `findAllByDueDateAndStatusNot` was deleted (dead code).

**`Task.dueDate` is now mandatory — reverses the 2026-09-07 "stays nullable" decision.** Deliberate product call: Align is narrowing to "every task is a dated commitment" so reminders and expiration apply uniformly. Enforced at three layers: `@NotNull` on `TaskRequest`/`TaskUpdateRequest` (REST via `@Valid`), `TaskServiceImpl.requireDueDate` throwing `BusinessException` (the AI-tool path skips bean validation — same pattern as `EventServiceImpl.validate`), and DB `NOT NULL` via `V17__make_due_date_required_on_tasks.sql` (`UPDATE tasks SET due_date = CURRENT_DATE WHERE due_date IS NULL` then `ALTER COLUMN ... SET NOT NULL` — backfill picks `CURRENT_DATE` so old undated rows show as "due today" with a day of grace before expiry rather than expiring instantly; on a fresh clone the `UPDATE` is a no-op). `create_task` tool schema gained `dueDate` in `required`. `dueTime` stays nullable (all-day tasks). `TaskExpirationIntegrationTest` lost its "undated task never expires" case (now unpersistable).

## Habit — `scheduledTime` wired to reminders

`Habit.scheduledTime` (added `V15`, 2026-09-08, originally "informational only") now also drives `HabitReminderJob`.

- `Habit.lastRemindedOn` (`LocalDate`, nullable), migration `V18__add_last_reminded_on_to_habits.sql` (`ADD COLUMN ... DATE NULL`, same shape as `V15`). Not in `HabitResponse`.
- `HabitService.findHabitsDueForReminder()` — mirrors `findHabitsAtRisk()` (`findAll()` + filter in Java): `scheduledTime != null AND scheduledTime <= now AND lastRemindedOn != today AND !isCompletedToday`. Filters ordered cheap-first so the per-habit `findByHabitOrderByDateDesc` runs only for habits past the other three. **No `streak > 0` filter** — this is "time to do it", not "at risk". `markReminded(UUID)` sets `lastRemindedOn = today` (`findById`, system sweep).
- `HabitReminderJob` (`scheduler`, per minute) — "Hora de tu hábito" / "Es hora de «X»." / `/habits`, no time in the copy (implicit "now").
- `updateHabit` does **not** reset `lastRemindedOn` when `scheduledTime` changes — deliberately unlike Task's `reArmReminderIfDueChanged`: a habit reminder is a recurring daily nudge, "already nudged today, next one tomorrow at the new time" is fine, and the 20:00 at-risk job still covers the day. Add the reset only if it proves annoying.

## Test coverage

Plain JUnit 5 + Mockito, no Spring context.

- `TaskServiceImplTest` — `requireDueDate` throws on create/update, `findTasksDueForReminder` delegates, `markReminderSent` sets+saves / not-found, `updateTask` re-arms on schedule change and not on an unrelated edit (via `doAnswer` simulating the mocked mapper mutating). Two existing tests fixed for the mandatory `dueDate` / renamed digest query.
- `HabitServiceImplTest` — `findHabitsDueForReminder` happy path + one exclusion per filter (no time / future time / already reminded / already completed), the first three asserting the completions query is never hit; `markReminded` sets+saves / not-found. `assumeNotNearMidnightUtc` guard (same idiom as `TaskExpirationIntegrationTest`) since the filter really evaluates `LocalTime.now(UTC)`.
- `TaskReminderJobTest` / `HabitReminderJobTest` (new) — 0/1/N candidates, `markReminderSent`/`markReminded` per item, same shape as `EventReminderJobTest`.
- Integration: none added. `TaskSpecifications.dueForReminder` is a SQL predicate (candidate for the same treatment as `expirable`); the Habit filter is Java-side and fully covered by the unit tests.


