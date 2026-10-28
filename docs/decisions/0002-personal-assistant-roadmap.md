# 0002 — Personal assistant roadmap

- **Status:** Accepted
- **Date:** 2026-08-24
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Decided 2026-08-24: Align stops adding new business domains for now (the Project/Goal/Note roadmap above is paused, not cancelled) and instead evolves the existing domains and the AI layer so the agent behaves like a real personal assistant, not just tool-calling over CRUD domains. Ordered phases — not to be parallelized, each one waits for the previous to reach a stable state:

1. **Persistent memory** — complete, see below.
2. **User context** — complete, see below.
3. **Authorization & confirmation** — complete, see below.
4. **Multi-step planning & execution** — paused, see below.
5. **Scheduler & events** — first case implemented, see below.
6. **Proactivity & notifications** — move from a purely reactive assistant to one that can detect situations worth surfacing and tell the user, unprompted. First cases implemented, see below.
7. **External integrations** (calendar, email, weather, ...) — kept decoupled from the agent itself, with provider-specific details encapsulated the same way `ai.llm.<provider>` already isolates Gemini.
8. **Voice** — treated as one more interface to the assistant, not a core architectural concern.
9. **NFC / physical triggers** — explored as a way to activate existing contexts or actions, not as a new domain.

Guiding constraint across every phase: no RAG, vector databases, Redis, Kafka, LangGraph, MCP, or multi-agent/microservice architectures unless a concrete Align problem first demonstrates the need — Align is a single-user personal project, and most of these solve scale problems it doesn't have. The test for every decision in this roadmap: **does this make Align a better assistant, or are we just adding technology?**

