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

Auth (JWT, register/login), Task (entity through AI tools), and Finance (entity through AI tools) are complete, each with test coverage. Habit, Project, Goal, and Note — the other domains in the platform's stated scope — have no code yet; they're future work, not started.

This is the backend MVP: Auth + Task + Finance, both as REST APIs and as AI tools callable through the chat agent. A frontend (Angular, separate repository from this one) is planned but not started — this file governs the backend only; the frontend repo will need its own CLAUDE.md once that work begins.

---

# Finance domain (`finance`) — complete (REST + AI tools)

Entity, DTOs, repository, mapper, service, controller, and AI tools are all implemented and tested — the same level of completeness as Task. This is the current state, so a new session can tell what's done versus what's still open:

- Done: `Transaction` entity, `TransactionType` / `Category` enums, and migration `V6__add_transactions.sql`. `Category` owns its `TransactionType` (e.g. `FOOD(EXPENSE)`, `SALARY(INCOME)`) so type is derived from category instead of being a second field a caller could set inconsistently.
- Done: DTO fields are decided. `TransactionRequest` / `TransactionUpdateRequest` take `amount`, `category`, `description`, `date` (validated with `@NotNull`/`@Positive`/`@Size`); `TransactionResponse` adds `id`, `type`, `createdAt`, `updatedAt`; `TransactionFilter` holds `type`, `category`, `from`, `to` (all optional); `FinancialSummaryResponse` holds `totalIncome`, `totalExpense`, `balance`.
- Done: `TransactionServiceImpl` implements the full `TransactionService` contract (`createTransaction`, `getTransactionById`, `getTransactions`, `updateTransaction`, `deleteTransaction`, `getSummary`). `type` is always re-derived from `category` on create/update, never trusted from the client. `TransactionMapper` (MapStruct) is implemented, with `type` excluded from the update mapping for the same reason.
- Resolved: the repository shape mismatch flagged previously is gone. `findByIdAndUser` now correctly returns `Optional<Transaction>` (was `Optional<List<Transaction>>`), and the overlapping `search` method was removed. Filtering (`getTransactions`, `getSummary`) goes through `TransactionSpecifications` + `JpaSpecificationExecutor` instead.
- Deliberate deviation from the Task pattern: `TransactionRepository` uses `JpaSpecificationExecutor` + a `TransactionSpecifications` builder instead of derived query methods (`findAllByUserAndX`) like `TaskRepository` does. Reason: `TransactionFilter` has four independent optional dimensions (type, category, date range); derived methods would need a combinatorial explosion of method signatures to cover every combination, where a `Specification` composes them from one place. `TaskRepository` keeps derived methods because it only filters on one optional field (`status`) — reach for `Specification` in a domain only once it has more than one or two optional filter dimensions, not by default.
- Done: `TransactionController` implements all 6 endpoints (`POST /api/transactions`, `GET /api/transactions/{id}`, `GET /api/transactions`, `PUT /api/transactions/{id}`, `DELETE /api/transactions/{id}`, `GET /api/transactions/summary`), same `ApiResponse<T>` + `HttpStatus` convention as `TaskController`.
- Deliberate deviation from the Task pattern: `getTransactions` and `getTransactionSummary` bind `TransactionFilter` with `@ModelAttribute` instead of individual `@RequestParam`s like `TaskController.getTasks` does for `status`. Reason: the filter has four optional fields reused across two endpoints — repeating four `@RequestParam`s in both would be real duplication, and `TransactionFilter` is already a plain neutral record with no hidden binding behavior. `TaskController` keeps `@RequestParam` because it only has one optional filter field — reach for `@ModelAttribute` once a filter has more than one or two fields shared across endpoints, not by default.
- Done: test coverage exists — `TransactionServiceImplTest` (business rules: type derivation on create/update, not-found handling, summary aggregation, filtered pagination) and `TransactionControllerTest` (delegation and status codes per endpoint). Both are plain JUnit 5 + Mockito + AssertJ unit tests with no Spring context, matching the style of `UpdateTaskToolTest` / `ConversationMemoryImplTest` — no `@SpringBootTest`/`MockMvc` precedent exists anywhere in the project yet.
- Done: AI tools (`create_transaction`, `list_transactions`, `get_financial_summary`) — see [Finance AI tools](#finance-ai-tools-aitool) for details, including their test coverage.

No next step defined for this domain — it's at parity with Task across the full stack. Further work here would only start once a real need shows up (e.g. `update_transaction`/`delete_transaction` through chat).

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

## Task AI tools (`ai.tool`)

Current coverage: `create_task`, `update_task`, `list_tasks`. `delete_task` is intentionally not implemented yet — deferred for this MVP, not an oversight; add it once there's a real need to delete tasks through chat.

`list_tasks` uses a fixed `Pageable` internally (size 20, sorted by `createdAt` DESC — same default as `TaskController.getTasks`), not exposed in its JSON schema. A chat request like "mostrame mis tareas pendientes" rarely needs explicit page/size control; add pagination parameters to the schema only if a real need to browse past the first page over chat shows up. It returns `List<TaskResponse>` (`Page#getContent()`), not the raw `Page`, so the LLM isn't handed pagination metadata (`pageable`, `totalElements`, etc.) it has no use for.

## Finance AI tools (`ai.tool`)

Current coverage: `create_transaction`, `list_transactions`, `get_financial_summary`. `update_transaction` and `delete_transaction` are intentionally not implemented yet — same reasoning as `delete_task`: no evidence yet that editing or deleting a transaction through chat is a real use case; add them once that need shows up.

`list_transactions` and `get_financial_summary` both take the same optional filter (`type`, `category`, `from`, `to`) and convert it directly to `TransactionFilter` — the fourth case in [Tool argument parsing](#tool-argument-parsing-aitool) above. `list_transactions` uses the same fixed `Pageable` as `list_tasks` (size 20, sorted by `createdAt` DESC, not exposed in the schema) and returns `List<TransactionResponse>`, not the raw `Page`. `get_financial_summary` has no pagination at all — it aggregates over whatever the filter matches, there's no list to page through.

`create_transaction`'s schema deliberately has no `type` property: `TransactionRequest` doesn't declare that field (`category` implies it). Advertising one the DTO doesn't have would make `execute()` throw `UnrecognizedPropertyException` the moment the LLM populated it, since the app's `ObjectMapper` fails on unknown properties by default — same reason `UpdateTaskTool`'s patch record needs `@JsonIgnoreProperties(ignoreUnknown = true)`. `CreateTransactionToolTest` guards this schema contract directly, so it can't regress silently.

Known gap: `SystemPromptBuilder`'s system prompt text still only mentions task management ("Ayudas al usuario a gestionar sus tareas"), not Finance. This doesn't block the Finance tools from being callable — the LLM receives tool specifications separately from the prompt text — but it's a stale description that undersells the agent's real capabilities. Worth fixing before adding a fourth domain's worth of tools on top.

## Conversation memory (`ai.memory`)

`ConversationMemory` persists and reloads conversation turns per user, so `AgentService` is not stateless across requests. It depends only on `Message` (provider-neutral), never on provider DTOs — same rule as the rest of the AI layer.

Design decisions currently in place:

- One conversation per user. There is no session/thread concept yet; introduce one only when a real requirement for multiple concurrent conversations shows up, not before.
- The system prompt is never persisted. `AgentService` rebuilds it from `SystemPromptBuilder` on every request and prepends it to the loaded history, so prompt changes never leave stale system messages in old users' history.
- Only a completed turn is persisted — the final assistant response with no pending tool calls. A turn that exhausts `MAX_STEPS` without resolving is discarded rather than saved half-finished.
- History is stored as a single serialized `List<Message>` per user (`ConversationHistory.historyJson`), not as one row per message. This is the minimal shape that solves today's requirement (remembering the conversation); move to a row-per-message model only if querying or trimming history becomes a real need, not preemptively.
- A failure to persist a turn (`append`) propagates and fails the request; the assistant's reply is not returned to the user in that case. This is intentional: silently swallowing a persistence failure would mask real infrastructure problems and let the agent forget a turn without anyone noticing.

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