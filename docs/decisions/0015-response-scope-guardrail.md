# 0015 — Response scope guardrail (legal liability)

- **Status:** Accepted
- **Date:** 2026-09-04
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Added 2026-09-04, prompted by a direct question about legal exposure ahead of the VPS production deploy — anticipatory hardening, not a reaction to any actual incident, same spirit as the JWT-expiration fix in `auth`.

Problem framing agreed: without a stated scope, the LLM has no reason to decline requests it can technically answer in prose but that belong to a licensed profession — a wrong answer there carries real legal exposure ("¿debería demandar a mi vecino?", specific tax/investment advice), unlike a wrong tool call, which is bounded and reversible by construction (`PendingAction`, correctable updates).

Decision, deliberately narrowed from a first draft: the boundary is **not** by domain/topic ("finanzas" in/out) but by **type of advice** — practical guidance grounded in the user's own data (tasks, habits, transactions, calendar) stays in scope, since that's literally what Align's domains already do; advice that substitutes for a licensed professional (medical, legal, psychological, or specific investment/tax strategy) stays out. The first draft vetoed "financiero" wholesale, which conflicted with Finance's own purpose (spending/budget guidance) — caught and corrected before implementing.

- **Prompt-only, not a structural guardrail** — same trade-off already made explicit in Phase 3 for `RiskLevel` (Option A vs B there): a classifier/filter step outside the LLM's own reasoning would be a hard guarantee, but is unbuilt infrastructure with zero evidence it's needed. Chosen deliberately as the MVP's first cut, not a placeholder — Align is single-user with no adversarial third party probing the boundary, and nothing here is an irreversible action the way `PendingAction`'s destructive tools are, so the risk doesn't yet justify the harder guarantee. Revisit only if a real instance of the model being talked past this line shows up.
- **Lives in `SystemPromptBuilder`, not a new component** — same "ambient context" rule from [Phase 2](0004-user-context.md) (small, bounded, relevant to nearly every message) that already governs date/timezone/memories. Not an action and not a domain capability, so never a candidate for a `Tool`.

Implemented: one paragraph appended to `SystemPromptBuilder.build`'s text block — states that practical, data-grounded advice is in scope, and that licensed-professional advice (médico/legal/psicológico, plus specific investment/tax strategy) is out of scope with a redirect instruction. `SystemPromptBuilderTest` gained a case asserting both halves of that line appear in the prompt.

Caught while writing the test, worth remembering for any future edit to this text block: an assertion spanning two lines of the block failed even though the text visibly "looked" contiguous in printed test output. Java text blocks preserve literal line breaks as real `\n` characters (no trailing `\` used anywhere in this block to suppress them), so a `.contains()` check crossing a wrapped-line boundary silently fails against a newline it can't see past. Fixed by keeping each assertion substring within a single physical source line — the same constraint every prior assertion in this test file already (implicitly) honored. Same "the build won't catch it" category as the `@Transactional`-override, FK-cascade, and Flyway-uppercase-`V` gotchas already documented elsewhere in this file.

Confirmed working end-to-end: manually tested against the live chat agent with both a practical-advice question (in scope) and a professional-advice question (out of scope) — the model made the distinction correctly.

Known gaps, deliberate for this first cut:

- The boundary is persuadable — a soft, prompt-level guarantee, not structurally enforced. Acceptable for a single-user personal project; not tested against adversarial phrasing, and not meant to be yet.
- "Psicológico" is vetoed outright, but Habit already touches wellbeing-adjacent territory (sleep, exercise routines), and the line between "hábito" and "salud mental" could blur if that domain grows (e.g. "no puedo dormir, me siento ansioso"). No tool or domain touches mental health today, so vetoing it is consistent — but this is a live tension flagged during design, not a closed decision.


