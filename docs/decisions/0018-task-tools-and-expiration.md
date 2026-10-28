# 0018 — Task AI tools and task expiration

- **Status:** Accepted
- **Date:** 2026-08 / 2026-09-07
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


## AI tools

Current coverage: `create_task`, `update_task`, `list_tasks`, `delete_task`. `delete_task` was added in Phase 3 as the proof case for the `PendingAction` confirmation gate (it's `RiskLevel.DESTRUCTIVE`), not because Task needed it for its own sake — see [Phase 3](0005-confirmation-gate.md). `update_task` is `RiskLevel.SAFE` (a wrong update is correctable by updating again); its code briefly drifted to `DESTRUCTIVE` and was corrected back 2026-09-01, now guarded by an explicit `risk()` assertion in `UpdateTaskToolTest`.

`list_tasks` uses a fixed `Pageable` internally (size 20, sorted by `createdAt` DESC — same default as `TaskController.getTasks`), not exposed in its JSON schema. A chat request like "mostrame mis tareas pendientes" rarely needs explicit page/size control; add pagination parameters to the schema only if a real need to browse past the first page over chat shows up. It returns `List<TaskResponse>` (`Page#getContent()`), not the raw `Page`, so the LLM isn't handed pagination metadata (`pageable`, `totalElements`, etc.) it has no use for.

Done (2026-09-04, part of the Calendar work — see [Calendar domain](0014-calendar-domain.md)): `list_tasks` gained an optional `dueFrom`/`dueTo` date filter so the agent can answer "¿qué tengo mañana?" reliably past the 20-row window. This crossed the second-optional-filter-dimension threshold and migrated `TaskRepository` to `JpaSpecificationExecutor` + a new `TaskSpecifications` builder, mirroring Finance; `TaskController.getTasks` moved from `@RequestParam status` to `@ModelAttribute TaskFilter`; `ListTasksTool`'s schema gained `dueFrom`/`dueTo` alongside `status`.

Done (2026-09-08): `create_task`'s schema now lists `dueDate` as `required`, and `TaskServiceImpl.requireDueDate` rejects a missing date with `BusinessException` on the tool path (bean validation is REST-only) — `Task.dueDate` is mandatory now, see [Timed reminders](0020-timed-reminders.md). `update_task`'s schema keeps `dueDate` optional (patch semantics; the merge coalesces against current state, which always has a date).


## Task expiration

Built on the `jet` branch, merged to `main` via PRs #30–32 (2026-09-07). A scheduled sweep that moves stale open tasks to a terminal `EXPIRED` state, plus a direct status-change endpoint. Follows the `PendingActionExpirationJob` precedent from [Phase 5](0008-scheduler.md): the `@Scheduled` component (`scheduler.TaskExpireJob`, `fixedRate = 60 * 60 * 1000`) does nothing but call `TaskService.expireOverdueTasks()`; the rule lives in the service.

- `TaskStatus` gained `CANCELLED` and `EXPIRED` (was `PENDING`/`IN_PROGRESS`/`COMPLETED`). No DB `CHECK` constraint on `tasks.status` (`V1` created it as a bare `VARCHAR(50)`), so the new values need no migration — `@Enumerated(EnumType.STRING)` just writes the name.
- `expireOverdueTasks()` — `@Transactional` (write override on the `readOnly = true` class), takes no `User`, system-wide sweep like `expireStale()`/`findDueReminders()`. Uses `TaskSpecifications.expirable(eligibleStatuses, today, now)`: `status IN (PENDING, IN_PROGRESS) AND (dueDate < today OR (dueDate = today AND dueTime IS NOT NULL AND dueTime < now))`. Sets each match to `EXPIRED` and `saveAll`s.
- `setTaskStatus(id, user, status)` + `PUT /api/tasks/{id}/{status}` — a thin direct status setter, ownership-checked via `findByIdAndUser`, `ResourceNotFoundException` on miss. Distinct from `updateTask` (which takes the full `TaskUpdateRequest`).

**`Task.dueDate` was made mandatory 2026-09-08 — reversing the 2026-09-07 "stays nullable" decision.** The original call kept it nullable (a task with no date can't be "overdue"; undated backlog / "someday" tasks are a real category; forcing a date invites fake ones). That was overturned deliberately when timed reminders landed: Align is narrowing to "every task is a dated commitment" so reminders *and* expiration apply uniformly, and the "someday" case is better served by a future status/list than by a null date. Now enforced at three layers (`@NotNull` DTOs, `TaskServiceImpl.requireDueDate` → `BusinessException` for the AI path, DB `NOT NULL` via `V17`); `create_task`'s schema lists `dueDate` as `required`; `V17` backfills existing null rows with `CURRENT_DATE`. `dueTime` **stays** nullable (all-day tasks) — `expireOverdueTasks` still only expires a same-day task once it's past its `dueTime`, or once the day rolls over if it has none. Full detail in [Timed reminders](0020-timed-reminders.md).

Test coverage: `TaskServiceImplTest.expireOverdueTasks_*` mocks the repository (the date/time predicate in `TaskSpecifications.expirable` isn't exercised there), but `TaskExpirationIntegrationTest` (see [Testing](0019-testing-strategy.md), added 2026-09-08) now runs that predicate against a real Postgres. `TransactionSpecifications` still has no equivalent integration test — a candidate for the same treatment.


