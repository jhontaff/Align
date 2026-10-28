# 0007 — Phase 4 — Multi-step planning (paused)

- **Status:** Paused
- **Date:** 2026-08-25
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Paused 2026-08-25, immediately after Phase 3 shipped, by applying the roadmap's own gate: "evolve beyond the current single tool-calling loop only once it stops being sufficient for a real case." Finding that closed the question for now: `AgentServiceImpl.chat`'s existing loop (`MAX_STEPS = 8`) already does reactive multi-step tool chaining within a single turn — the LLM sees each tool's result and can call another tool before finalizing (e.g. `list_habits` → `complete_habit` already resolves in one round-trip today). What Phase 4 would add on top of that — an explicit up-front plan instead of reactive step-by-step decisions, rollback on partial failure, or continuity of a task across multiple conversation turns — has no real case behind it: no request has hit the 8-step ceiling, needed to survive across turns, or needed a plan shown to the user before executing. Revisit only when one does; the reasoning here stays valid meanwhile, same as the paused Project/Goal/Note domain roadmap above.

