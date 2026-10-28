# 0025 — Undo today's habit completion (`uncomplete_habit`)

- **Status:** Accepted
- **Date:** 2026-09-04 (commit `ecb8111`)
- **Origin:** reconstructed from code on 2026-10-27. It was never documented when it was built.

## Decision

- `HabitService.uncompleteHabit(User, UUID)` checks ownership through `findByIdAndUser`, then calls `HabitCompletionRepository.deleteByHabitAndDate(habit, today)`, with today = `LocalDate.now(timezone)`. It recomputes `currentStreak`, `longestStreak` and `isCompletedToday` the same way `completeHabit` does, and has its own `@Transactional` write override.
- It only removes **today's** completion. Past days can't be undone through it.
- It is idempotent, like `completeHabit`: deleting a row that doesn't exist is a no-op.
- REST: `DELETE /api/habits/{id}/completions` returns `200` with the updated `HabitResponse`, mirroring `POST /{id}/completions`.
- AI tool `uncomplete_habit`: the single-id case, `RiskLevel.SAFE`. Its description tells the model to call `list_habits` first when it only knows the habit's name.

## Why `SAFE`

[0005](0005-confirmation-gate.md) classifies risk by whether the prior state can be reconstructed, not by the CRUD verb. The deleted row is exactly "completed today", and `complete_habit` recreates it. That puts this tool in the same class as `update_task`, not `delete_habit`.

## Tests

`HabitServiceImplTest` (not found; deletes today's completion and recomputes the streak), `HabitControllerTest`, `UncompleteHabitToolTest`.
