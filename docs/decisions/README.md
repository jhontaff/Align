# Decision records

One file per architectural or product decision, numbered in rough chronological order. `CLAUDE.md` holds only the rules in force and a short current status. The *why* behind those rules lives here.

## Conventions

- **New decision → new file.** Use `NNNN-kebab-title.md` with the next free number and this header:
  ```
  # NNNN — Title
  - **Status:** Proposed | Accepted | Paused | Dormant | Superseded by NNNN
  - **Date:** YYYY-MM-DD
  ```
  Then cover context, the decision, the alternatives you rejected, and the consequences/known gaps.
- **Never rewrite the body of an accepted record.** If reality changes, append a dated bullet under `## Later notes` at the end. If the decision itself is reversed, write a new record and set the old one's status to `Superseded by NNNN`.
- **Rules go to `CLAUDE.md`.** When a decision produces a rule that future code must follow (for example "`byte[]` without `@Lob`"), add the rule in one line to `CLAUDE.md` and link back here.
- Records 0001–0023 were migrated verbatim from the old monolithic `CLAUDE.md` on 2026-10-27. Records 0024–0026 were reconstructed from code, because those decisions were never written down.

## Index

| # | Decision | Status |
|---|---|---|
| [0001](0001-domain-roadmap.md) | Domain roadmap (Project / Goal / Note) | Paused |
| [0002](0002-personal-assistant-roadmap.md) | Personal assistant roadmap (phases 1–9) | Accepted |
| [0003](0003-persistent-memory.md) | Phase 1 — Persistent memory (`UserMemory`) | Accepted |
| [0004](0004-user-context.md) | Phase 2 — User context (ambient prompt context, `align.timezone`) | Accepted |
| [0005](0005-confirmation-gate.md) | Phase 3 — Confirmation gate (`RiskLevel`, `PendingAction`) | Accepted |
| [0006](0006-auth-jwt.md) | Auth — JWT expiration handling, user-id subject | Accepted |
| [0007](0007-multi-step-planning-paused.md) | Phase 4 — Multi-step planning | Paused |
| [0008](0008-scheduler.md) | Phase 5 — Scheduler (`@Scheduled`, `PendingAction` TTL) | Accepted |
| [0009](0009-proactive-push-notifications.md) | Phase 6 — Proactivity & Web Push | Accepted |
| [0010](0010-production-deployment.md) | Production deployment on a VPS | Accepted |
| [0011](0011-schema-reconciliation-v12.md) | Schema reconciliation — `V12` | Accepted |
| [0012](0012-finance-domain.md) | Finance domain and its AI tools | Accepted |
| [0013](0013-habit-domain.md) | Habit domain and its AI tools | Accepted |
| [0014](0014-calendar-domain.md) | Calendar domain | Accepted |
| [0015](0015-response-scope-guardrail.md) | Response scope guardrail (legal liability) | Accepted |
| [0016](0016-byok.md) | BYOK per-user LLM credentials | Dormant |
| [0017](0017-backups.md) | Database backups | Accepted |
| [0018](0018-task-tools-and-expiration.md) | Task AI tools and task expiration | Accepted |
| [0019](0019-testing-strategy.md) | Testing strategy — unit + Testcontainers | Accepted |
| [0020](0020-timed-reminders.md) | Timed reminders; `Task.dueDate` mandatory | Accepted |
| [0021](0021-user-domain.md) | User domain — profile & settings | Accepted |
| [0022](0022-password-reset.md) | Password reset by email (Resend) | Accepted (email body superseded by 0024) |
| [0023](0023-conversation-memory-and-chat-history.md) | Conversation memory and chat history | Accepted |
| [0024](0024-reset-email-thymeleaf-template.md) | Reset email rendered with Thymeleaf | Accepted — rationale pending |
| [0025](0025-uncomplete-habit.md) | Undo today's habit completion | Accepted |
| [0026](0026-finance-monthly-chart.md) | Daily current-vs-previous-month chart | Accepted — open points |
