    # CLAUDE.md

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

---

# Architectural principles

- The LLM is an infrastructure detail, never the center of the system.
- Business logic must never depend on Gemini, OpenAI or any specific provider.
- The domain must remain completely independent from the AI layer.
- Provider-specific DTOs must never leave `ai.llm.<provider>`.
- Only provider-neutral models (`LlmRequest`, `LlmResponse`, `Message`, `ToolCall`, etc.) may be used outside provider packages.
- Prefer composition over conditionals.
- Prefer extracting responsibilities over creating large service classes.
- Every business domain follows the same architectural conventions.
- New domains should integrate with the existing architecture instead of introducing new patterns.

---

# Domain module layout

Every business domain (`task`, `finance`, `habit`, `project`, `goal`, `note`, `user`, `auth`, ...) follows exactly the same internal structure.

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
- MapStruct performs mappings.
- Every `ServiceImpl` declares `@Transactional(readOnly = true)` at the class level, with an explicit `@Transactional` override on every write method. Skipping the override on even one mutating method isn't just an inconsistency — PostgreSQL's JDBC driver honors the read-only hint at the connection level (`SET TRANSACTION READ ONLY`), so any `INSERT`/`UPDATE`/`DELETE` inside that method fails outright at runtime. Learned the hard way while building Habit — check this specifically when reviewing any new domain's service.

Cross-cutting components belong in shared packages (`common`, `config`, etc.), never duplicated inside business domains.

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

Business domains communicate through services, never by bypassing domain boundaries.

No domain receives special architectural treatment unless there is a compelling reason.

---

# Current backend status

Auth (JWT, register/login), Task (entity through AI tools), and Finance (entity through AI tools) are complete, each with test coverage — Task's (`TaskServiceImplTest`, `TaskControllerTest`) was the one gap here, closed after the fact rather than being part of the original build. This was the original backend MVP scope, both as REST APIs and as AI tools callable through the chat agent.

Post-MVP, Habit is the first new domain added, and it's now at full parity with Task and Finance: entity through AI tools, complete and tested (see [Habit domain](#habit-domain-habit-complete-rest-ai-tools) below). Project, Goal, and Note — the remaining domains in the platform's stated scope — have no code yet.

Domain roadmap decided while designing Habit: **Project is next** — it will be the first domain that composes over another domain's service (grouping Tasks, possibly Transactions for a budget) instead of standing alone, the first real exercise of the "domains communicate through services" principle below. **Goal is deliberately deferred**, not scheduled: it risks being the same "container that tracks progress toward something" abstraction as Project under a different name, and deciding its shape before Project exists would be guessing — revisit once Project is built and a genuine need for a separate concept shows up. **Note** stays out of scope until a concrete need to jot something down through chat appears; as a plain CRUD-of-text domain with no business rules of its own, it wouldn't teach anything Task hasn't already. **Budget** (spending limits per category) and **Reminders/Notifications** (scheduled/background jobs) came up as candidate capabilities but were explicitly not adopted as top-level domains: Budget fits as a new entity inside `finance` rather than a sibling domain, and Reminders is an infrastructure concern (async/scheduled work) the project hasn't needed yet.

**Paused 2026-08-24**: this Project/Goal/Note domain roadmap is on hold, not cancelled — the reasoning above stays valid for whenever domain work resumes. Current priority shifted to evolving Align into a real personal assistant on top of the existing domains instead of adding new ones; see [Personal assistant roadmap](#personal-assistant-roadmap) below.

---

# Finance domain (`finance`) — complete (REST + AI tools)

Entity, DTOs, repository, mapper, service, controller, and AI tools are all implemented and tested — the same level of completeness as Task. This is the current state, so a new session can tell what's done versus what's still open:

- Done: `Transaction` entity, `TransactionType` / `Category` enums, and migration `V6__add_transactions.sql`. `Category` owns its `TransactionType` (e.g. `FOOD(EXPENSE)`, `SALARY(INCOME)`) so type is derived from category instead of being a second field a caller could set inconsistently.
- Done: DTO fields are decided. `TransactionRequest` / `TransactionUpdateRequest` take `amount`, `category`, `description`, `date` (validated with `@NotNull`/`@Positive`/`@Size`); `TransactionResponse` adds `id`, `type`, `createdAt`, `updatedAt`; `TransactionFilter` holds `type`, `category`, `from`, `to` (all optional); `FinancialSummaryResponse` holds `totalIncome`, `totalExpense`, `balance`.
- Done: `TransactionServiceImpl` implements the full `TransactionService` contract (`createTransaction`, `getTransactionById`, `getTransactions`, `updateTransaction`, `deleteTransaction`, `getSummary`). `type` is always re-derived from `category` on create/update, never trusted from the client. `TransactionMapper` (MapStruct) is implemented, with `type` excluded from the update mapping for the same reason.
- Deliberate deviation from the Task pattern: `TransactionRepository` uses `JpaSpecificationExecutor` + a `TransactionSpecifications` builder instead of derived query methods (`findAllByUserAndX`) like `TaskRepository` does. Reason: `TransactionFilter` has four independent optional dimensions (type, category, date range); derived methods would need a combinatorial explosion of method signatures to cover every combination, where a `Specification` composes them from one place. `TaskRepository` keeps derived methods because it only filters on one optional field (`status`) — reach for `Specification` in a domain only once it has more than one or two optional filter dimensions, not by default.
- Done: `TransactionController` implements all 6 endpoints (`POST /api/transactions`, `GET /api/transactions/{id}`, `GET /api/transactions`, `PUT /api/transactions/{id}`, `DELETE /api/transactions/{id}`, `GET /api/transactions/summary`), same `ApiResponse<T>` + `HttpStatus` convention as `TaskController`.
- Deliberate deviation from the Task pattern: `getTransactions` and `getTransactionSummary` bind `TransactionFilter` with `@ModelAttribute` instead of individual `@RequestParam`s like `TaskController.getTasks` does for `status`. Reason: the filter has four optional fields reused across two endpoints — repeating four `@RequestParam`s in both would be real duplication, and `TransactionFilter` is already a plain neutral record with no hidden binding behavior. `TaskController` keeps `@RequestParam` because it only has one optional filter field — reach for `@ModelAttribute` once a filter has more than one or two fields shared across endpoints, not by default.
- Done: test coverage exists — `TransactionServiceImplTest` (business rules: type derivation on create/update, not-found handling, summary aggregation, filtered pagination) and `TransactionControllerTest` (delegation and status codes per endpoint). Both are plain JUnit 5 + Mockito + AssertJ unit tests with no Spring context, matching the style of `UpdateTaskToolTest` / `ConversationMemoryImplTest` — no `@SpringBootTest`/`MockMvc` precedent exists anywhere in the project yet.
- Done: AI tools (`create_transaction`, `list_transactions`, `get_financial_summary`) — see [Finance AI tools](#finance-ai-tools-aitool) for details, including their test coverage.

No next step defined for this domain — it's at parity with Task across the full stack. Further work here would only start once a real need shows up (e.g. `update_transaction`/`delete_transaction` through chat).

---

# Habit domain (`habit`) — complete (REST + AI tools)

Entity, DTOs, repository, mapper, service, controller, AI tools, and tests are all implemented — the same level of completeness as Task and Finance. This is the current state, so a new session can tell what's done versus what's still open:

- Done: two entities, not one. `Habit` (`user`, `name` — the definition, extends `BaseEntity` like every other entity, no redeclared `id`) and `HabitCompletion` (`habit` FK, `date` — one row per day marked done, also extends `BaseEntity`). Same reasoning as `Category`/`Transaction` in Finance: a "definition" entity that changes rarely stays separate from an "event" entity that grows without bound. Modeling this as a single `Habit` with a `lastCompletedAt` field would lose the history needed to compute a streak. Migration `V7__add_habits.sql` creates both tables (`habits`, `habit_completions`) with `UNIQUE(habit_id, date)` enforced at the database level — Hibernate never generates schema here (`ddl-auto=validate`, Flyway owns the DDL), so that constraint has to live in the migration; a `@Table(uniqueConstraints = ...)` annotation would have no effect either way.
- Done: frequency scope for this first version is daily-only, on purpose. No frequency field exists on `Habit` — every habit is expected every day. Weekly/custom-day recurrence was considered and deferred; add it only once a real habit that isn't daily shows up.
- Done: two streak numbers are calculated on every read, both never stored. `calculateStreak` (current) walks `HabitCompletion` rows (`HabitCompletionRepository.findByHabitOrderByDateDesc`, most recent first), counting consecutive days back from the most recent one, with a "grace day" rule: if the last completion was yesterday (not necessarily today), the streak is still alive — it only resets to zero once a full day passes with no completion. `calculateLongestStreak` scans the same list for the longest run anywhere in the history — no grace day, no early exit, since it isn't anchored to "today". Both read from one fetch per call site: `getHabits`/`getHabitById`/`updateHabit`/`completeHabit` all extract `completions` into a local variable before calling both methods, instead of querying the repository twice. The two numbers are meant to diverge: a single stale completion from days ago resets `currentStreak` to `0` while `longestStreak` stays `1`, and an active-but-short streak can still report a much larger `longestStreak` from an older run that already broke. A denormalized field on `Habit` for either was considered and rejected: it can desync from the completion history, and at personal-tracker scale, recomputing from a handful of rows per request isn't a real performance problem.
- Done: `completeHabit` is idempotent by design, not an error path. Calling it twice for the same day is a no-op the second time (checked via `HabitCompletionRepository.existsByHabitAndDate`) rather than throwing — a duplicate "ya lo hice hoy" from a chat retry shouldn't surface as a user-facing error.
- Deliberate deviation from the Task/Transaction pattern: `HabitRequest` is reused for both `createHabit` and `updateHabit` — there's no separate `HabitUpdateRequest`. Reason: `Habit` has exactly one editable field (`name`); a dedicated update DTO would be a distinction without a difference. Revisit if `Habit` ever grows a second field where create and update rules diverge.
- Done: `HabitMapper.toResponse(Habit habit, int currentStreak, int longestStreak)` maps from **three** source parameters — neither streak is a column on `Habit`; both are computed by the service and passed in explicitly (`@Mapping(target = "currentStreak", source = "currentStreak")`, same for `longestStreak`). First mapper in the project that needs MapStruct's multi-source-parameter support, since Task/Transaction map 1:1 from a single entity.
- Done: `HabitController` implements all 6 endpoints (`POST /api/habits`, `GET /api/habits/{id}`, `GET /api/habits`, `PUT /api/habits/{id}`, `DELETE /api/habits/{id}`, `POST /api/habits/{id}/completions`), same `ApiResponse<T>` convention as `TaskController`/`TransactionController`. The completions endpoint returns `200 OK`, not `201 CREATED`, even though it's a `POST` — it's idempotent and may be a no-op, so "a resource was created" isn't always true.
- Fixed during development, now captured as a general rule in [Domain module layout](#domain-module-layout): a class-level `@Transactional(readOnly = true)` needs an explicit `@Transactional` override on every write method, or Postgres rejects the writes outright.
- Done: test coverage — `HabitServiceImplTest` (17 tests: CRUD + not-found paths, `completeHabit` idempotency, current-streak edge cases — zero with no completions, consecutive-day counting, the grace-day rule, reset after a gap, a broken streak not resuming past a gap — plus two `longestStreak`-specific cases: it survives `currentStreak` resetting to zero after inactivity, and it can exceed `currentStreak` when an older, longer run already broke) and `HabitControllerTest` (6 tests: delegation and status code per endpoint). Same style as `TransactionServiceImplTest`/`TransactionControllerTest` — plain JUnit 5 + Mockito + AssertJ, no Spring context. Streak tests use dates relative to `LocalDate.now()` rather than fixed dates, since the behavior under test depends on what day it is.
- Done: AI tools (`create_habit`, `list_habits`, `complete_habit`) — see [Habit AI tools](#habit-ai-tools-aitool) for details, including their test coverage. Confirmed working end-to-end against the live chat agent.

No next step defined for this domain — it's at parity with Task and Finance across the full stack.

---

# Personal assistant roadmap

Decided 2026-08-24: Align stops adding new business domains for now (the Project/Goal/Note roadmap above is paused, not cancelled) and instead evolves the existing domains and the AI layer so the agent behaves like a real personal assistant, not just tool-calling over CRUD domains. Ordered phases — not to be parallelized, each one waits for the previous to reach a stable state:

1. **Persistent memory** — complete, see below.
2. **User context** — date/time, timezone, preferences, relevant memory, conversation context. Decide what should be ambient context (always present) versus what should be resolved via Tools (fetched on demand).
3. **Authorization & confirmation** — distinguish read / write / destructive / external-effect actions. Decide when the agent may act automatically versus when it must ask for confirmation first.
4. **Multi-step planning & execution** — evolve beyond the current single tool-calling loop only once it stops being sufficient for a real case; keep it as simple as possible until then.
5. **Scheduler & events** — let Align react to time and system events. Never use the LLM for deterministic problems plain logic already solves.
6. **Proactivity & notifications** — move from a purely reactive assistant to one that can detect situations worth surfacing and tell the user, unprompted.
7. **External integrations** (calendar, email, weather, ...) — kept decoupled from the agent itself, with provider-specific details encapsulated the same way `ai.llm.<provider>` already isolates Gemini.
8. **Voice** — treated as one more interface to the assistant, not a core architectural concern.
9. **NFC / physical triggers** — explored as a way to activate existing contexts or actions, not as a new domain.

Guiding constraint across every phase: no RAG, vector databases, Redis, Kafka, LangGraph, MCP, or multi-agent/microservice architectures unless a concrete Align problem first demonstrates the need — Align is a single-user personal project, and most of these solve scale problems it doesn't have. The test for every decision in this roadmap: **does this make Align a better assistant, or are we just adding technology?**

## Phase 1 — Persistent memory (complete)

Problem framing agreed: `ConversationMemory` (existing, see [Conversation memory](#conversation-memory-aimemory) below) is turn-by-turn conversational history — append-only, read-as-a-whole, tied to the LLM round-trip format. Long-term memory is a different concept: durable facts about the user that survive independent of any single conversation, individually addressable (create/update/delete one fact at a time, not read-the-whole-blob). That difference in access pattern is why it can't reuse `ConversationHistory`'s single-JSON-blob shape — it needs one row per memory.

This isn't a new business domain — no REST controller is planned, since nothing today needs to manage memories outside chat. It lives in `ai.memory` alongside `ConversationMemory`, under a distinct name (`UserMemory`) so the two kinds of memory (conversational vs. long-term) aren't confused with each other.

Scope discipline: what gets captured is limited to what doesn't already fit a structured domain. "Quiero ahorrar más" belongs in Finance, "quiero dormir temprano" is a Habit — free-text memory is for interaction preferences and durable biographical/contextual facts that don't map to an existing entity, not a dumping ground for things the domains already model better.

Decisions made:

- **Capture is explicit only** — a tool the LLM calls when the user asks to remember something or it's clearly implied, not automatic background extraction analyzing every turn. Automatic extraction would need an extra LLM call per turn and a fuzzy "what's memorable" heuristic with no evidence yet that it's needed. Revisit only if manually asking to be remembered becomes real friction.
- **Retrieval has no semantic search or relevance ranking.** Single user, small volume of facts — listing everything is proportional at this scale, the same reasoning behind this assistant's own memory system (a fully-loaded index, no embedding search). Automatically injecting all memories into every `SystemPromptBuilder` call is explicitly deferred to Phase 2 (User context) — Phase 1 only builds the retrieval mechanism (a `list_memories` tool), not the automatic wiring into every request's context.
- **`UserMemory` entity stays free text, no category/type field.** YAGNI — add a category column later via migration if a real grouping need shows up, not preemptively.
- **`update_memory` is an explicit operation, not delete-then-recreate.** Editing content and replacing identity are semantically different operations; `updatedAt` (already free via `BaseEntity`) and room for future history/versioning motivated keeping update distinct rather than folding it into delete+create.
- **Identifying *which* memory to update is delegated to the LLM, not solved structurally.** No embedding-similarity matching — `update_memory` requires an exact `id` (never a free-text search), and the service verifies id ownership (`findByIdAndUser`), same pattern as everywhere else in the project. The tool's `description()` must instruct the model to call `list_memories` first and ask the user for clarification instead of guessing when uncertain — same idiom `complete_habit` already uses for `habitId` discovery. This reduces wrong-target risk but doesn't eliminate it; genuine protection (e.g. requiring confirmation before applying) is Phase 3's problem, not Phase 1's — `update_memory` is flagged as a natural first candidate for that future confirmation gate.

Implemented, in this order — entity + migration + repository, service, AI tools, tests:

- Done: `UserMemory` entity (`ai.memory`, no redeclared `id`, extends `BaseEntity` like every other entity) — `user` (`@ManyToOne`, lazy, not null) and `content` (`columnDefinition = "TEXT"`, not `varchar(255)`, since a memory is free-form text, unlike `Habit.name`'s short label). Migration `V8__add_user_memories.sql` creates `user_memories` with the same FK-to-`users` shape as every other per-user table, deliberately without an explicit index on `user_id` — same precedent as `habits`, not `habit_completions`, since nothing here queries at that volume yet. Caught during review: the entity's `@Table` name and the migration's `CREATE TABLE` initially didn't match (`user_memory` vs `user_memories`), and the migration file was first named with a lowercase `v8__...`, which Flyway's default `V`-prefix matching silently ignores — combined, the app wouldn't have started under `ddl-auto=validate`. Both fixed before moving on; worth double-checking table-name-in-code vs. table-name-in-migration and the uppercase `V` prefix specifically on any future migration, the same way the `@Transactional` override rule gets checked on every new service.
- Done: `UserMemoryRepository` — `findByUserOrderByCreatedAtDesc` (recent-first, same reasoning as `HabitRepository`: whoever reads this list, human or LLM, cares about recent facts first) and `findByIdAndUser`.
- Done: `UserMemoryService` / `impl` — `remember(user, content)`, `list(user)`, `update(user, id, content)`, `forget(user, id)`, same class-level `@Transactional(readOnly = true)` + per-write-method override as every other `ServiceImpl`. No `MemoryRequest` DTO and no MapStruct: `content` is a single primitive argument (same reasoning as `completeHabit(User, UUID)` needing no request DTO), and the AI layer has no MapStruct precedent anywhere (`ai.agent`'s `toChatTurn` maps manually too). Output DTO is `ai.memory.dto.MemoryResponse(id, content, createdAt)` — no `updatedAt`, since nothing yet needs to know when a memory was last edited; exists mainly so the entity's lazy `user` reference never gets handed to Jackson for serialization. `update`/`forget` throw `ResourceNotFoundException` (not `IllegalArgumentException`) on a missing/foreign id — this one matters more than the usual "match the existing exception type" nit, because `AgentServiceImpl.runTool` only special-cases `BusinessException`/`ResourceNotFoundException` into a clean `{"error": ...}` tool message; anything else falls into the generic catch-all and the LLM gets a useless "no se pudo completar la operación" instead of the specific not-found message it needs to recover (e.g. call `list_memories` again).
- Done: AI tools (`remember_fact`, `list_memories`, `update_memory`, `forget_fact`) in `ai.tool`. `remember_fact` is the single-field case, `list_memories` the no-argument case, `update_memory`/`forget_fact` the id-based case per [Tool argument parsing](#tool-argument-parsing-aitool) — none of the four convert a `Map` into a DTO via `objectMapper.convertValue`, so unlike `create_transaction` there's no schema-vs-DTO mismatch risk to guard with a test. `remember_fact`'s `description()` explicitly tells the model not to use it for things an existing domain already models (a savings goal is Finance, a sleep routine is Habit) — the scope discipline above only holds if the tool description enforces it, since the LLM never reads this file. `update_memory` is the first tool in the project whose `description()` hard-requires "call `list_memories` first, only act when confident, otherwise ask the user" rather than offering it as a convenience — mitigates but doesn't eliminate wrong-target risk, real protection is Phase 3's confirmation gate. `forget_fact` is the first tool in the project with no payload (`Tool<Void>`, `ToolResult<>(null, ...)`) — confirmed `ToolExecutionServiceImpl`/`AgentServiceImpl.runTool` serialize a `null` payload with no special-casing needed.
- Done: test coverage — `UserMemoryServiceImplTest` (6 tests: CRUD + not-found paths for `update`/`forget`, same style as `HabitServiceImplTest`) and one test per tool (`RememberFactToolTest`, `ListMemoriesToolTest`, `UpdateMemoryToolTest`, `ForgetFactToolTest` — argument conversion + delegation, same style as `CompleteHabitToolTest`/`ListHabitsToolTest`). Plain JUnit 5 + Mockito + AssertJ, no Spring context.

No REST controller — not planned, nothing outside chat needs to manage memories yet. No automatic injection of memories into `SystemPromptBuilder`/every request's context — that wiring is Phase 2's decision (User context), not this phase's; today a memory is only surfaced when the LLM explicitly calls `list_memories`.

---

# AI architecture

The AI layer orchestrates business capabilities.

Responsibilities are:

- Agent → orchestrates conversations.
- LLM → decides what tool to use.
- ToolExecutionService → executes tool calls.
- ToolRegistry → discovers available tools.
- Tool → adapts AI requests to business services.
- Domain Services → contain business rules.

The implementation should always respect this separation.

New AI capabilities should normally be implemented as new `Tool` implementations instead of adding logic to `AgentService`.

The architecture diagram in `graphify-out/graph.html` reflects the current implementation. The principles defined in this document take precedence over the diagram.

## Tool argument parsing (`ai.tool`)

A tool's raw arguments (`ToolContext.arguments()`) are a `Map<String, Object>` deserialized from the LLM's JSON tool call — every value arrives as `String`/`null` (or a nested `Map`/`List`), never as an already-typed enum or date. Never cast these values directly (`(Priority) arguments.get(...)`); it throws `ClassCastException`. Convert through Jackson instead, following the shape the tool actually needs:

- **Full-replace tools** (e.g. `CreateTaskTool`) convert straight to the DTO the domain service expects: `objectMapper.convertValue(arguments, TaskRequest.class)`.
- **Partial-update tools** (e.g. `UpdateTaskTool`) convert to a private, all-nullable "patch" record scoped to the tool, annotated `@JsonIgnoreProperties(ignoreUnknown = true)` (fields read separately before the conversion, like the resource id, are otherwise-unknown properties to the patch type and would fail deserialization without it), then merge each field against the current state to build the full DTO the service expects.
- **Query/filter tools** (e.g. `ListTasksTool`) that only take a single simple field don't need either pattern above — an explicit conversion on that one field (`TaskStatus.valueOf(...)`) is enough. Reach for the patch-record pattern only once there's real merge logic to justify it.
- **Filter DTOs with several optional fields, no merge needed** (e.g. `ListTransactionsTool`, `GetFinancialSummaryTool`): when the filter is already an all-nullable, neutral record (like `TransactionFilter`) and there's no "current state" to merge against — each call is a fresh filter, not a partial update — convert the whole `Map` straight to the filter record with `objectMapper.convertValue(...)`, the same call full-replace tools use. This differs from the single-field case above only in field count; it differs from the patch-record case in that there's nothing to merge, so `@JsonIgnoreProperties(ignoreUnknown = true)` isn't needed either, as long as the schema's properties match the filter record's fields exactly.
- **No-argument tools** (e.g. `ListHabitsTool`): when the underlying service call takes nothing beyond the authenticated user (`HabitService.getHabits(User)` has no filter or pagination at all), the schema declares an empty `properties` object and `execute()` never touches `context.arguments()`. Simplest case of the four — nothing to parse, nothing to convert.

## Task AI tools (`ai.tool`)

Current coverage: `create_task`, `update_task`, `list_tasks`. `delete_task` is intentionally not implemented yet — deferred for this MVP, not an oversight; add it once there's a real need to delete tasks through chat.

`list_tasks` uses a fixed `Pageable` internally (size 20, sorted by `createdAt` DESC — same default as `TaskController.getTasks`), not exposed in its JSON schema. A chat request like "mostrame mis tareas pendientes" rarely needs explicit page/size control; add pagination parameters to the schema only if a real need to browse past the first page over chat shows up. It returns `List<TaskResponse>` (`Page#getContent()`), not the raw `Page`, so the LLM isn't handed pagination metadata (`pageable`, `totalElements`, etc.) it has no use for.

## Finance AI tools (`ai.tool`)

Current coverage: `create_transaction`, `list_transactions`, `get_financial_summary`. `update_transaction` and `delete_transaction` are intentionally not implemented yet — same reasoning as `delete_task`: no evidence yet that editing or deleting a transaction through chat is a real use case; add them once that need shows up.

`list_transactions` and `get_financial_summary` both take the same optional filter (`type`, `category`, `from`, `to`) and convert it directly to `TransactionFilter` — the fourth case in [Tool argument parsing](#tool-argument-parsing-aitool) above. `list_transactions` uses the same fixed `Pageable` as `list_tasks` (size 20, sorted by `createdAt` DESC, not exposed in the schema) and returns `List<TransactionResponse>`, not the raw `Page`. `get_financial_summary` has no pagination at all — it aggregates over whatever the filter matches, there's no list to page through.

`create_transaction`'s schema deliberately has no `type` property: `TransactionRequest` doesn't declare that field (`category` implies it). Advertising one the DTO doesn't have would make `execute()` throw `UnrecognizedPropertyException` the moment the LLM populated it, since the app's `ObjectMapper` fails on unknown properties by default — same reason `UpdateTaskTool`'s patch record needs `@JsonIgnoreProperties(ignoreUnknown = true)`. `CreateTransactionToolTest` guards this schema contract directly, so it can't regress silently.

## Habit AI tools (`ai.tool`)

Current coverage: `create_habit`, `list_habits`, `complete_habit` — no deferred tools here, this is the full set the domain needs today. `create_habit` is the full-replace case (`objectMapper.convertValue(arguments, HabitRequest.class)`), `list_habits` is the no-argument case (fifth bullet in [Tool argument parsing](#tool-argument-parsing-aitool) above — `HabitService.getHabits` takes only the user, nothing to parse), and `complete_habit` is the single-field case (`UUID.fromString((String) context.arguments().get("habitId"))`, no patch record needed since there's nothing to merge).

`complete_habit`'s `description()` explicitly tells the LLM to call `list_habits` first if it doesn't already know the habit's id — the user speaks in habit names, not UUIDs, and no tool exposes lookup-by-name. This mirrors the existing two-call flow `update_task` already relies on for `taskId`; it isn't a new capability of the agent, just the first time Habit needs it spelled out in a tool description.

Done: `SystemPromptBuilder` reworded and trimmed rather than extended with another domain name. Instead of enumerating domains ("tareas y sus finanzas personales", the pattern that needed an edit every time a new domain's tools shipped), it now says the agent helps with "su vida personal usando las herramientas disponibles" — generic enough that adding Project/Goal/Note tools later won't require touching this file again. The sentence explicitly telling the model to call a tool when one applies was also cut: Gemini's function-calling already decides tool use from the schemas sent in `tools`, so the sentence was redundant, not load-bearing. Confirmed working end-to-end against the live chat agent after the trim — tool calls (including the new Habit ones) still trigger reliably.

## Conversation memory (`ai.memory`)

`ConversationMemory` persists and reloads conversation turns per user, so `AgentService` is not stateless across requests. It depends only on `Message` (provider-neutral), never on provider DTOs — same rule as the rest of the AI layer.

Design decisions currently in place:

- One conversation per user. There is no session/thread concept yet; introduce one only when a real requirement for multiple concurrent conversations shows up, not before.
- The system prompt is never persisted. `AgentService` rebuilds it from `SystemPromptBuilder` on every request and prepends it to the loaded history, so prompt changes never leave stale system messages in old users' history.
- Only a completed turn is persisted — the final assistant response with no pending tool calls. A turn that exhausts `MAX_STEPS` without resolving is discarded rather than saved half-finished.
- History is stored as a single serialized `List<Message>` per user (`ConversationHistory.historyJson`), not as one row per message. This is the minimal shape that solves today's requirement (remembering the conversation); move to a row-per-message model only if querying or trimming history becomes a real need, not preemptively.
- A failure to persist a turn (`append`) propagates and fails the request; the assistant's reply is not returned to the user in that case. This is intentional: silently swallowing a persistence failure would mask real infrastructure problems and let the agent forget a turn without anyone noticing.

## Agent chat & history (`ai.agent`)

Current coverage: `POST /api/agent/chat` (existing) and `GET /api/agent/history` (new — added because the frontend needs it to restore the conversation view on load). Both endpoints are thin on `AgentController`, delegating to `AgentService`.

- Done: `AgentResponse` moved from a flat file in `ai.agent` into `ai.agent.dto`, alongside two new records: `ChatTurn(String role, String content)` and `ChatHistoryResponse(List<ChatTurn> turns)`. `AgentService.getHistory(User user)` returns `ChatHistoryResponse`, never the neutral `Message` type the AI layer uses internally — `Message` is shaped for the LLM round-trip (roles `system`/`user`/`assistant`/`tool`, `toolCalls`, `toolCallId`), not for a chat UI, so it doesn't cross the REST boundary directly. Same reasoning as why domain entities never leave a controller as-is.
- Done: `AgentServiceImpl.getHistory` delegates to `conversationMemory.loadHistory(user)` and maps each `Message` to a `ChatTurn` via a private exhaustive `switch` (`toChatTurn`) — the same idiom `GeminiLlmClient.toGeminiContent` already uses to map `Message` toward the provider. No MapStruct: the AI layer has no precedent for it anywhere (MapStruct is a domain-module convention for entity↔DTO mapping, not used inside `ai`).
- Deliberate design decision: `toChatTurn` throws `IllegalStateException` if it encounters a `SystemMessage` or `ToolMessage`. In practice `ConversationMemory` should never hand back those types — `AgentServiceImpl.chat` only ever persists `List.of(userTurn, assistant)` on the no-tool-calls branch, so persisted history is always alternating `UserMessage`/`AssistantMessage`. Throwing turns a broken invariant into a loud, visible failure instead of silently dropping the unexpected turn — same "fail loudly" preference already applied to `ConversationMemory.append` above.
- Done, project-wide fix driven by this feature: `GlobalExceptionHandler`'s catch-all `handleException` (which is what handles the `IllegalStateException` above, since it has no dedicated handler) now logs the exception before returning its generic 500. It previously logged nothing, so any unmapped exception anywhere in the app — not just this one — would reach the client as a bare 500 with zero trace in the logs, defeating the point of failing loudly. Same "log detail internally, respond generically to the client" pattern `AgentServiceImpl.runTool` already used for unexpected tool failures.
- Done: test coverage in `AgentServiceImplTest` — the happy path (`UserMessage`/`AssistantMessage` → `ChatTurn` list) and the invariant-violation path (`ToolMessage` in history → `IllegalStateException`). Plain JUnit 5 unit tests using the existing `SpyConversationMemory` double, no Spring context, matching the rest of the AI layer's tests. No `AgentControllerTest` exists for either endpoint (`chat` was never tested at the controller layer either) — history is only tested at the service layer.
- Known gap: `getHistory` returns the entire persisted history every time, no pagination or trimming — it inherits the "single serialized blob, not row-per-message" shape already noted in Conversation memory above. Revisit only if history grows large enough for that to matter.

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

The objective is to help the developer become a better software architect, not to maximize code generation.

Always preserve architectural consistency throughout the project.

When architectural decisions are unclear, prefer the solution that improves long-term maintainability and developer understanding over the one that minimizes the amount of code written.