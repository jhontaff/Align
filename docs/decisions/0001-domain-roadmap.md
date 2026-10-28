# 0001 — Domain roadmap (Project / Goal / Note)

- **Status:** Paused
- **Date:** 2026-08 (paused 2026-08-24)
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.

Post-MVP, Habit is the first new domain added, and it's now at full parity with Task and Finance: entity through AI tools, complete and tested (see [Habit domain](0013-habit-domain.md) below). Project, Goal, and Note — the remaining domains in the platform's stated scope — have no code yet.

Domain roadmap decided while designing Habit: **Project is next** — it will be the first domain that composes over another domain's service (grouping Tasks, possibly Transactions for a budget) instead of standing alone, the first real exercise of the "domains communicate through services" principle below. **Goal is deliberately deferred**, not scheduled: it risks being the same "container that tracks progress toward something" abstraction as Project under a different name, and deciding its shape before Project exists would be guessing — revisit once Project is built and a genuine need for a separate concept shows up. **Note** stays out of scope until a concrete need to jot something down through chat appears; as a plain CRUD-of-text domain with no business rules of its own, it wouldn't teach anything Task hasn't already. **Budget** (spending limits per category) and **Reminders/Notifications** (scheduled/background jobs) came up as candidate capabilities but were explicitly not adopted as top-level domains: Budget fits as a new entity inside `finance` rather than a sibling domain, and Reminders is an infrastructure concern (async/scheduled work) the project hasn't needed yet.

**Paused 2026-08-24**: this Project/Goal/Note domain roadmap is on hold, not cancelled — the reasoning above stays valid for whenever domain work resumes. Current priority shifted to evolving Align into a real personal assistant on top of the existing domains instead of adding new ones; see [Personal assistant roadmap](0002-personal-assistant-roadmap.md) below.

