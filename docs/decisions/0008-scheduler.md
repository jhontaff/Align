# 0008 — Phase 5 — Scheduler & events

- **Status:** Accepted
- **Date:** 2026-08
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Problem framing agreed: unlike Phases 1–3, this phase has no single feature to build — it's a mechanism ("let Align react to time") that different domains plug into over time, not a one-shot deliverable. The first concrete case, chosen specifically because it doesn't need Phase 6 to have value: `PendingAction` rows left `PENDING` forever have no TTL today — a stale, still-confirmable destructive action with no context, growing the table without bound. Chosen over Task due-date or Habit daily reminders because those need a channel to actually surface something to the user, which is Phase 6's job — this case is pure deterministic housekeeping with no user-facing output at all, so it doesn't have to wait.

Decisions made:

- **Spring's `@Scheduled` (fixed rate), not Quartz or any cron library.** Same guiding constraint as the rest of this roadmap — Align is a single-instance personal app, nothing here needs persistence-backed scheduling that survives missed runs across restarts. Revisit only if a real need (e.g. catch-up after downtime) shows up.
- **Mechanism/business-rule split mirrors "controllers delegate."** The `@Scheduled` component (`PendingActionExpirationJob`, in a new `scheduler` package — sibling to `ai`/`common`, not nested inside `ai.tool`, since the scheduler is meant to trigger multiple domains over time, not just this one) does nothing but call `PendingActionService.expireStale()`. The actual rule — what counts as stale, what happens to it — lives in the service, same as every other domain's business logic.
- **`expireStale()` takes no `User` argument** — the first service method in the project not scoped to one user. A sweep runs for the whole system, not "on behalf of" anyone, so the `findByIdAndUser` ownership pattern used everywhere else doesn't apply here.
- **New `PendingActionStatus.EXPIRED` value, not a reuse of `REJECTED`.** Weighed explicitly: reusing `REJECTED` costs zero schema change but conflates two different meanings — "the user said no" vs. "nobody responded in time." Chosen to add the real value because it's needed the moment the known Phase 3 gap (no `GET /api/agent/pending-actions` listing endpoint) gets built — that listing would need to tell the two apart, and retrofitting the distinction later would mean reclassifying already-persisted rows.
- **TTL is a single global config value** (`align.pending-action.ttl-hours=24`), same precedent as `align.timezone` from Phase 2 — not per-user, proportional to Align still being single-operator.
- **Sweep runs hourly** (`fixedRate = 60 * 60 * 1000`), chosen relative to the 24h TTL: bounds the delay between an action crossing the TTL and actually being marked `EXPIRED` to ~1h, insignificant against 24h and cheap at this volume (one derived query, usually zero rows).

Implemented:

- Done: `PendingActionStatus.EXPIRED` and `PendingActionRepository.findByStatusAndCreatedAtBefore(PendingActionStatus, Instant)`.
- Done: `PendingActionService.expireStale()` / impl — finds stale `PENDING` rows via the repository, sets each to `EXPIRED`, `saveAll`s them. Same `@Transactional` override discipline as every other write method on a `readOnly = true` service.
- Done: `PendingActionServiceImpl` switched from `@RequiredArgsConstructor` to an explicit constructor to carry `@Value("${align.pending-action.ttl-hours}") int ttlHours` — same idiom `AgentServiceImpl` established in Phase 2: a `@Value`-sourced primitive plus tests that construct the service directly means `@RequiredArgsConstructor` alone isn't enough.
- Done: `scheduler` package + `PendingActionExpirationJob` (`@Component`, `@Scheduled`), delegating to `expireStale()`. `@EnableScheduling` added to `AlignApplication` — required for `@Scheduled` to fire at all; easy to build the job correctly and still have it silently never run without this.
- Done: `application.properties` gained `align.pending-action.ttl-hours=24`.
- Done: test coverage — 2 new cases in `PendingActionServiceImplTest` (`expireStale` marks stale `PENDING` rows `EXPIRED` and persists them via `saveAll`; the cutoff passed to the repository is verified against the configured `ttlHours` via `ArgumentCaptor`). No dedicated test for `PendingActionExpirationJob` itself — thin, delegates, same precedent as controllers never getting a test for their one-line delegation.

Known gaps, deliberate for this first cut:

- ~~No other domain plugs into the scheduler yet~~ — resolved by Phase 6: `HabitAtRiskJob` and `TaskDueReminderJob` now plug into `scheduler`, see below.
- No catch-up/backfill if the app is down when a scheduled run would have fired — `@Scheduled(fixedRate = ...)` just skips missed runs. Acceptable for housekeeping with a 24h TTL and hourly sweeps; revisit only if a future scheduled job needs a stronger guarantee.


