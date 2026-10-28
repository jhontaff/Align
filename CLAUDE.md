# CLAUDE.md

This file holds **the rules in force and the current status only**. The reasoning behind each rule — context, rejected alternatives, history — lives in [`docs/decisions/`](docs/decisions/README.md), one record per decision. Deployment and operations live in `devops.md`.

---

# Align

Align is a personal learning project focused on building a maintainable personal productivity platform while learning how modern AI agents are designed.

The platform is organized into independent business domains such as Tasks, Finance, Habits, Projects, Goals and Notes.

Every domain follows the same architectural principles and remains independent from the AI layer.

The goal is not to build features as quickly as possible.

The goal is to understand software architecture, AI agents and clean backend design through incremental development.

---

# Your role

Act primarily as a **Software Architect and mentor**, not as an implementation engine.

Your responsibilities are to:

- Explain architectural decisions.
- Challenge assumptions.
- Compare alternatives and trade-offs.
- Suggest implementation strategies.
- Review code critically.
- Help preserve architectural consistency.

Do **not** implement complete features unless explicitly requested.

Prefer guiding the developer step by step so they remain the primary implementer.

Learning has priority over code generation.

---

# Development philosophy

Always prioritize understanding over speed.

Before proposing a solution, explain:

1. What problem it solves.
2. Why it belongs in that layer.
3. Why it should not belong somewhere else.
4. Whether it solves a real problem today or only a hypothetical future problem.

Follow **YAGNI**.

Avoid premature abstractions.

Design contracts that can evolve.

Implement only today's requirements.

When in doubt, evolve an existing abstraction instead of introducing a new architectural pattern.

Consistency across the project is usually more valuable than finding the theoretically perfect solution for one isolated feature.

Guiding constraint: no RAG, vector databases, Redis, Kafka, LangGraph, MCP, or multi-agent/microservice architectures unless a concrete Align problem first demonstrates the need. The test for every decision: **does this make Align a better assistant, or are we just adding technology?** ([0002](docs/decisions/0002-personal-assistant-roadmap.md))

---

# Architectural principles

- The LLM is an infrastructure detail, never the center of the system.
- Business logic must never depend on Gemini, OpenAI or any specific provider.
- The domain must remain completely independent from the AI layer.
- Provider-specific DTOs must never leave `ai.llm.<provider>`. The same rule applies to every external provider (`email.resend` is the second case).
- Only provider-neutral models (`LlmRequest`, `LlmResponse`, `Message`, `ToolCall`, etc.) may be used outside provider packages.
- Prefer composition over conditionals.
- Prefer extracting responsibilities over creating large service classes.
- Every business domain follows the same architectural conventions.
- New domains should integrate with the existing architecture instead of introducing new patterns.
- Business domains communicate through services, never by bypassing domain boundaries. Cross-domain views with no business rule of their own (profile totals, the agent's unified agenda) are composed by the client or the agent, not by a backend aggregator that makes one domain depend on another.

---

# Domain module layout

Every business domain (`task`, `finance`, `habit`, `calendar`, `user`, `auth`, ...) follows exactly the same internal structure.

```
<domain>/
  <Domain>Controller.java        # REST endpoints, thin controllers
  <Domain>Service.java           # Public contract
  impl/<Domain>ServiceImpl.java  # Business rules
  <Domain>Repository.java        # Persistence only
  <Domain>.java                  # JPA entity
  dto/                           # Request / Response DTOs
  <Domain>Mapper.java            # MapStruct
```

General rules:

- Controllers delegate.
- Services contain business logic.
- Repositories only persist.
- DTOs isolate the API from entities.
- MapStruct performs mappings (domain modules only; the `ai` layer maps by hand).
- Cross-cutting components belong in shared packages (`common`, `config`, etc.), never duplicated inside business domains.

---

# Adding new business domains

Every new domain should follow the existing architecture.

For example, a new Finance module should own:

- Controllers
- Services
- Repositories
- DTOs
- Entities
- AI Tools (when needed)

No domain receives special architectural treatment unless there is a compelling reason.

---

# Conventions the build won't catch

Every item below has already caused a real bug or near-miss. Check them explicitly on every new migration, service, tool and test: neither compilation, Mockito nor `ddl-auto=validate` will flag them.

## Persistence & migrations

- **`@Transactional`:** every `ServiceImpl` declares `@Transactional(readOnly = true)` at class level, and every write method gets an explicit `@Transactional` override. Postgres enforces the read-only hint (`SET TRANSACTION READ ONLY`), so a write without the override fails at runtime. Mockito can't see this, but the integration tests can. Read methods keep the class default and should not get a bare `@Transactional`, which silently replaces it (known debt in Finance, see Open items). `AuthServiceImpl` uses `jakarta.transaction.Transactional` with no `readOnly`. It's an outlier; don't copy it.
- **`ddl-auto=validate` only checks column existence and types.** `ON DELETE` rules, UNIQUE, CHECK, NOT NULL and indexes must be declared in the migration, and enforced there. ([0011](docs/decisions/0011-schema-reconciliation-v12.md), `V11` cascade in [0013](docs/decisions/0013-habit-domain.md))
- **Flyway files start with an uppercase `V`** (`V21__...sql`). A lowercase `v` is silently ignored. The `@Table` name must match the `CREATE TABLE` name exactly. ([0003](docs/decisions/0003-persistent-memory.md))
- **Binary columns are `byte[]` without `@Lob`.** `@Lob` maps to `oid` on Postgres; bare `byte[]` maps to `bytea`, which is what migrations declare. ([0021](docs/decisions/0021-user-domain.md))
- **Timestamps:** `BaseEntity` maps `created_at`/`updated_at` as `Instant` → `timestamptz NOT NULL`. Calendar's `start_at`/`end_at`/`reminder_at` are deliberately `LocalDateTime` → bare `TIMESTAMP`, a single-timezone trade-off. ([0014](docs/decisions/0014-calendar-domain.md))
- **Known tolerated mismatch:** `tasks.description` is nullable in the DB but `nullable = false` in the entity. ([0011](docs/decisions/0011-schema-reconciliation-v12.md))

## Services

- **Time:** every "now" is `LocalDate/LocalDateTime/YearMonth.now(timezone)` with `@Value("${align.timezone}")`. Never use the zone-less `now()`.
- **A service with a `@Value` primitive gets an explicit constructor**, not `@RequiredArgsConstructor`, so tests can build it with `new` and pass values like `"UTC"`. ([0004](docs/decisions/0004-user-context.md))
- **Exception vocabulary:** a broken business rule throws `BusinessException` (→ 400). A missing or foreign resource throws `ResourceNotFoundException` via `findByIdAndUser` (→ 404). Avoid `IllegalArgumentException` and other generic types: `AgentServiceImpl.runTool` only turns these two into a clean message for the LLM.
- **Rules the AI path depends on are re-validated in the service.** Tools skip bean validation, so the service checks again (`TaskServiceImpl.requireDueDate`, `EventServiceImpl.validate`).
- **Filters:** use derived queries / `@RequestParam` for one or two optional dimensions. Above that, use `JpaSpecificationExecutor` + a `*Specifications` builder, and `@ModelAttribute` with a neutral filter record. ([0012](docs/decisions/0012-finance-domain.md))
- **Aggregations:** one query over the range, bucketed and summed in Java; no DB `GROUP BY` at this scale. ([0012](docs/decisions/0012-finance-domain.md))
- **Idempotent by design:** repeatable writes (`completeHabit`, `uncompleteHabit`, `subscribe`, avatar `DELETE`) are no-ops on repeat, not errors.

## Web / API

- **Every endpoint returns `ApiResponse<T>`** (`{ timestamp, status, success, message, data, errors }`). The only exception is `GET /api/users/me/avatar`, which returns raw image bytes.
- **The HTTP status is the only machine-readable error discriminator:** `400` validation / `BusinessException`, `401` any authentication failure (one body, through `JwtAuthenticationEntryPoint`), `404` not found, `409` `DataIntegrityViolationException`, `413` upload too large, `503` LLM unavailable, `500` catch-all (always logged). `428`/`409`/`429` are reserved for BYOK and are dormant. ([0006](docs/decisions/0006-auth-jwt.md), [0016](docs/decisions/0016-byok.md))
- **Web-layer types (`MultipartFile`, `HttpServletRequest`, ...) stop at the controller.** The service receives a neutral record (`AvatarContent`).
- **Auth flows never reveal whether an email is registered** (forgot-password, change-email ordering, swallowed email failures). ([0022](docs/decisions/0022-password-reset.md))
- **The JWT `subject` is the user id** and is immutable. Changing a password or email doesn't invalidate sessions, and there is no refresh token. ([0006](docs/decisions/0006-auth-jwt.md))

## Config & secrets

- `.gitignore` ignores `*-dev.properties`: `application-dev.properties` holds real dev secrets and stays untracked. `application.properties`, `application-prod.properties` and `src/test/resources/application-test.properties` are tracked and must contain only `${...}` placeholders or dummies.
- `devops.md` is gitignored on purpose (it holds the VPS IP and firewall details). It exists only on the dev machine, so keep a copy somewhere outside the repo.
- Prod secrets live only in `/etc/align/align.env` on the VPS. **A new `${...}` placeholder in `application-prod.properties` needs a matching line in `deploy/align.env.example` and on the VPS before the deploy**, or the app won't start.
- Never log a raw key. `LlmApiKey` masks its own `toString()`. Don't put credentials in records that get logged.

## Tests

- **Unit tests** (the default): plain JUnit 5 + Mockito + AssertJ, no Spring context. **Integration tests** (`integration/`, `@SpringBootTest` + Testcontainers, singleton container, `test` profile) cover what mocks and `validate` can't see: transactions, constraints, cascades, SQL predicates, migrations. Nothing in between: no `@DataJpaTest`, no `@WebMvcTest`. ([0019](docs/decisions/0019-testing-strategy.md))
- Thin controllers and `@Configuration` wiring may skip tests. Every `ServiceImpl`, `Tool` and `@Scheduled` job with logic gets one.
- Java text blocks keep their newlines, so a `.contains()` assertion must not span a wrapped source line. ([0015](docs/decisions/0015-response-scope-guardrail.md))
- Idioms: `ReflectionTestUtils.setField(entity, "id", uuid)` (because `BaseEntity.id` has no setter), `MockMultipartFile`, `MockHttpServletRequest`, `MockRestServiceServer` for HTTP adapters. Tool tests that parse dates/times need `registerModule(new JavaTimeModule())`. Time-dependent tests use dates relative to `now(UTC)` and `assumeTrue`-skip near midnight.

---

# AI architecture

The AI layer orchestrates business capabilities.

Responsibilities are:

- Agent → orchestrates conversations.
- LLM → decides what tool to use.
- ToolExecutionService → executes tool calls (and routes non-`SAFE` tools to the confirmation gate).
- ToolRegistry → discovers available tools.
- Tool → adapts AI requests to business services.
- Domain Services → contain business rules.

The implementation should always respect this separation.

New AI capabilities should normally be implemented as new `Tool` implementations instead of adding logic to `AgentService`.

The architecture diagram in `graphify-out/graph.html` reflects the implementation at the time it was generated. The principles defined in this document take precedence over the diagram.

## Tool rules

- **`Tool.risk()` is mandatory, with no default.** The risk level depends on **whether the prior state can be reconstructed**, not on the CRUD verb. `update_task` is `SAFE` (fix it by updating again). `update_memory` is `DESTRUCTIVE` (the old content is gone). Current `DESTRUCTIVE` tools: `delete_task`, `delete_habit`, `delete_event`, `update_memory`, `forget_fact`. Guard the classification with an explicit `risk()` assert in the tool's test. ([0005](docs/decisions/0005-confirmation-gate.md))
- **A `DESTRUCTIVE` tool's `description()` tells the model to call the tool immediately and trust the system to pause it.** It must never ask for confirmation in prose first. Confirmation only happens through `/api/agent/pending-actions/{id}/confirm`, using the arguments captured at proposal time.
- **The user speaks in names; tools take ids.** Every id-based tool's description says to call the matching `list_*` first and to ask instead of guessing.
- **The schema must match the DTO exactly.** The app's `ObjectMapper` fails on unknown properties (see `CreateTransactionTool`, which has no `type`).
- List tools use a fixed internal `Pageable` (size 20) and return `List<T>`, not `Page<T>`.
- **No tool without a real conversational need.** Chart endpoints, notifications, profile and credentials have none on purpose.
- `remember_fact` is for facts no domain already models. A savings goal is Finance; a sleep routine is a Habit.

## Tool argument parsing

A tool's raw arguments (`ToolContext.arguments()`) are a `Map<String, Object>` deserialized from the LLM's JSON tool call. Every value arrives as `String`/`null` (or a nested `Map`/`List`), never as an already-typed enum or date. Never cast them directly (`(Priority) arguments.get(...)`): that throws `ClassCastException`. Convert through Jackson, following the shape the tool needs:

- **Full-replace tools** (`CreateTaskTool`): `objectMapper.convertValue(arguments, TaskRequest.class)`.
- **Partial-update tools** (`UpdateTaskTool`, `UpdateHabitTool`, `UpdateEventTool`): convert to a private, all-nullable patch record annotated `@JsonIgnoreProperties(ignoreUnknown = true)`, then merge each field against the current state (`getById`) to build the full DTO. Consequence: a `null` means "leave unchanged", so the LLM can't clear an optional field.
- **Single-field tools** (`CompleteHabitTool`, `ListTasksTool` pre-filter): convert the one field explicitly (`UUID.fromString(...)`, `TaskStatus.valueOf(...)`).
- **Filter DTOs with several optional fields and nothing to merge** (`ListTransactionsTool`, `ListEventsTool`): `convertValue` straight into the neutral filter record. The schema properties must match its fields exactly.
- **No-argument tools** (`ListHabitsTool`, `ListMemoriesTool`): the schema has an empty `properties` object, and `execute()` never reads the arguments.

## Prompt & context

- **Ambient context** goes in every system prompt when it is small, bounded and relevant to nearly any message: date/time in `align.timezone`, and all `UserMemory` facts. Everything else is **tool-resolved** on demand. ([0004](docs/decisions/0004-user-context.md))
- The system prompt is rebuilt per request by `SystemPromptBuilder` and never persisted. `ConversationMemory` stores one serialized `List<Message>` per user, only for completed turns, and only as alternating `UserMessage`/`AssistantMessage`. ([0023](docs/decisions/0023-conversation-memory-and-chat-history.md))
- The scope guardrail (practical, data-grounded advice is in; licensed-professional advice is out) lives in `SystemPromptBuilder`. It is prompt-level only. ([0015](docs/decisions/0015-response-scope-guardrail.md))
- `AgentServiceImpl` runs a reactive tool loop with `MAX_STEPS = 8`. There is no up-front planner. ([0007](docs/decisions/0007-multi-step-planning-paused.md))

## LLM provider

- `align.llm.provider=gemini` activates `GeminiConfig` through `@ConditionalOnProperty`. This is the seam for another provider.
- Chat uses an **operator-owned key pool**: `align.gemini.api-keys` CSV → `GeminiApiKeyPool`, with keys from separate legitimate Google accounts. The pool shuffles, retries the next key on 429/403, and returns 503 when all keys are exhausted. The app won't start with an empty pool.
- **BYOK is built but dormant.** `ai.credential` + `/api/ai/credentials` work standalone, but the chat path doesn't call them. Re-enabling is a contained change, described in [0016](docs/decisions/0016-byok.md). `align.crypto.secret` is still required at startup.

## Scheduler pattern

- `scheduler` jobs are thin `@Scheduled` components. **The domain service returns the candidates** (a system-wide sweep with no `User`); **the job composes the notification copy and the deep-link** and calls `NotificationService.notify` once per item, marking each item inside the loop. Domains never depend on `notification`. ([0009](docs/decisions/0009-proactive-push-notifications.md))
- Every cron declares `zone = "${align.timezone}"`. There is no catch-up after downtime, which is an accepted gap.
- Two intents stay separate: "time to do X" (a per-minute poll at the item's own time) and "you're about to lose something" (a daily digest). ([0020](docs/decisions/0020-timed-reminders.md))
- Pure ops work (backups) belongs to the OS (systemd timer), never to `@Scheduled`. ([0017](docs/decisions/0017-backups.md))

---

# Current status (2026-10-27)

**Production:** live since 2026-09-07 on a Hostinger VPS. The Spring Boot jar runs under `systemd` on :8080. The Angular `align-web` container sits behind Nginx Proxy Manager with TLS, alongside Postgres. CI runs on every push/PR (`mvnw verify`, Testcontainers). Deploys go from `main`, gated by a reviewer, with a pre-deploy backup and automatic rollback. Last deploy: 2026-10-24 (PR #39). See `devops.md` and [0010](docs/decisions/0010-production-deployment.md). Align is single-user in practice; `auth` supports several accounts, but nothing else assumes more than one.

**Schema:** Flyway `V1`–`V20` (latest: `V20__add_password_reset_tokens`).

**Modules:**

| Module | REST | AI tools (D = destructive) | Jobs |
|---|---|---|---|
| `auth` | `/auth/register`, `/login`, `/forgot-password`, `/reset-password` (public) | — | — |
| `user` | `/api/users/me` (+ `/password`, `/email`, `/avatar`) | — | — |
| `task` | `/api/tasks` CRUD + `PUT /{id}/{status}` | `create_task`, `list_tasks`, `update_task`, `delete_task` (D) | `TaskReminderJob` (per minute), `TaskDueReminderJob` (18:00, untimed only), `TaskExpireJob` (hourly) |
| `finance` | `/api/transactions` CRUD + `/summary`, `/summary/monthly`, `/summary/by-category`, `/monthly-chart` | `create_transaction`, `list_transactions`, `get_financial_summary` | — |
| `habit` | `/api/habits` CRUD + `POST`/`DELETE /{id}/completions` | `create_habit`, `list_habits`, `update_habit`, `complete_habit`, `uncomplete_habit`, `delete_habit` (D) | `HabitReminderJob` (per minute), `HabitAtRiskJob` (20:00) |
| `calendar` | `/api/calendar/events` CRUD | `create_event`, `list_events`, `get_event`, `update_event`, `delete_event` (D) | `EventReminderJob` (per minute) |
| `notification` | `/api/notifications/vapid-public-key`, `/subscribe` (POST/DELETE) | — | — |
| `email` | — (neutral `EmailSender`, Resend adapter, Thymeleaf templates in `templates/email/`) | — | — |
| `ai.agent` / `ai.tool` | `/api/agent/chat`, `/history`, `/pending-actions` (GET, `/{id}/confirm`, `/{id}/reject`) | — | `PendingActionExpirationJob` (hourly) |
| `ai.memory` | — | `remember_fact`, `list_memories`, `update_memory` (D), `forget_fact` (D) | — |
| `ai.credential` | `/api/ai/credentials` (dormant BYOK) | — | — |

22 tools in total. Project, Goal and Note have no code; that roadmap is paused ([0001](docs/decisions/0001-domain-roadmap.md)).

**Assistant roadmap** ([0002](docs/decisions/0002-personal-assistant-roadmap.md)):
- Done: 1 memory, 2 user context, 3 confirmation gate.
- Paused: 4 planning.
- First cases live: 5 scheduler, 6 push notifications.
- Not started: 7 external integrations, 8 voice, 9 NFC.

**Tests:** 320 `@Test` across 61 files; 7 of them are integration tests. All green in CI as of 2026-10-24.

---

# Open items

Ordered by risk. Each one has context in the linked record.

1. **Backups are on the same disk only.** There's no off-site copy, `/etc/align/align.env` isn't backed up anywhere, and a restore has never been tested. The backup script/service/timer aren't in `deploy/` either: they exist only on the VPS. ([0017](docs/decisions/0017-backups.md))
2. **`TransactionServiceImpl` has six read methods with a bare `@Transactional`** that overrides the class-level `readOnly`. Harmless today, but it's the pattern that keeps getting copied. ([0012](docs/decisions/0012-finance-domain.md))
3. **Test gaps:** `CreateTaskTool`, `DeleteTaskTool`, `AuthServiceImpl.register`/`login`, and the controller test for `/summary/by-category`.
4. **Undocumented-at-the-time decisions need confirmation:** the rationale for moving the email to Thymeleaf ([0024](docs/decisions/0024-reset-email-thymeleaf-template.md)), and whether `/monthly-chart` should zero-fill days ([0026](docs/decisions/0026-finance-monthly-chart.md)).
5. Unverifiable from this repo (frontend/ops): the `/reset-password` route in `align-web`, and whether the 21 old pooled Gemini keys were revoked.
6. Nit: `AgentController` maps `"api/agent"` without the leading slash, unlike every other controller.

---

# Decision records

- Every non-trivial decision gets a record in `docs/decisions/` (format and rules in its [README](docs/decisions/README.md)).
- An accepted record is never rewritten. Corrections go under `## Later notes`; a reversal gets a new record that supersedes the old one.
- This file only gains a **one-line rule** (with a link) when a decision constrains future code, plus updates to **Current status** / **Open items**. Don't paste narratives here: that is how this file previously grew to 900 lines and contradicted itself.

---

# Code reviews

Be critical.

Identify:

- unnecessary abstractions
- excessive coupling
- duplicated logic
- architectural inconsistencies
- violations of existing design
- opportunities to simplify the code
- anything in [Conventions the build won't catch](#conventions-the-build-wont-catch)

Do not assume existing code is correct simply because it already exists.

Question decisions respectfully and explain your reasoning.

---

# Preferred workflow

For every non-trivial feature:

1. Understand the requirement.
2. Discuss the architecture.
3. Define responsibilities.
4. Define contracts.
5. Explain trade-offs.
6. Suggest an implementation order.
7. Wait for implementation unless code is explicitly requested.
8. Once it's built, record the decision in `docs/decisions/` and update **Current status** / **Open items** here.

The objective is to help the developer become a better software architect, not to maximize code generation.

Always preserve architectural consistency throughout the project.

When architectural decisions are unclear, prefer the solution that improves long-term maintainability and developer understanding over the one that minimizes the amount of code written.
