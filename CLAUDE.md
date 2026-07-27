# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Align** — Spring Boot 3.5 / Java 21 backend. A task manager with a JWT-secured REST API, plus an AI agent layer that lets a user chat in natural language and have an LLM call tools (e.g. create tasks) on their behalf. Provider-agnostic LLM integration (Gemini implemented — the only provider wired for the MVP).

## Working principles (apply to every task in this repo)

- **MVP first.** Prioritize the smallest change that makes the feature work end-to-end over a "complete" design. Don't gold-plate a layer (e.g. add caching, retries, or extra abstraction) before the feature it supports actually works.
- **Solid but not over-engineered architecture.** Keep the existing layering (Controller → Service interface → ServiceImpl → Repository, DTOs + Mapper) consistent for new features. Don't introduce a new architectural pattern for one feature when the existing one fits.
- **Clean, maintainable, reusable code.** Follow the conventions already in the codebase (see Architecture below) rather than introducing new ones. Prefer extending an existing interface/abstraction (e.g. `Tool<T>`, `LlmClient`) over duplicating logic.
- **Explain step by step.** When implementing a non-trivial change, briefly walk through what's being done and why (e.g. "1. add the DTO, 2. add the mapper, 3. wire the service, 4. expose the endpoint") rather than just dropping a diff silently. Keep it concise — a short numbered list, not an essay.

## Common commands

```bash
# Run the app (localhost:1010)
./mvnw spring-boot:run

# Build (compiles, runs tests, packages jar)
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AgentServiceImplTest

# Run a single test method
./mvnw test -Dtest=AgentServiceImplTest#methodName
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

`application.properties` alone has no datasource or secrets — a Spring profile must be active. Run with `-Dspring.profiles.active=dev` (reads `application-dev.properties`, gitignored, holds local Postgres creds + a real Gemini API key for local testing). `application-prod.properties` pulls everything from env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `GEMINI_API_KEY`) instead.

Requires a running PostgreSQL instance; Flyway migrations in `src/main/resources/db/migration` run automatically on startup (`ddl-auto=validate`, so schema changes must go through a new migration, never through Hibernate auto-DDL). Swagger UI is at `/swagger-ui.html`.

## Architecture

### Domain module layout

Code is organized by domain package (`auth`, `user`, `task`, `ai`), each following the same shape:

```
<domain>/
  <Domain>Controller.java      # REST endpoints, thin
  <Domain>Service.java         # interface
  impl/<Domain>ServiceImpl.java
  <Domain>Repository.java      # Spring Data JPA
  <Domain>.java                # JPA entity (extends common/model/BaseEntity)
  dto|model/                   # Request/Response records or classes
  <Domain>Mapper.java          # MapStruct, entity <-> DTO
```

New features should follow this same shape rather than introducing ad hoc structures.

### Cross-cutting (`common/`)

- `common.response.ApiResponse<T>` — uniform envelope for every REST response (success/error).
- `common.exception.*` + `GlobalExceptionHandler` (`@RestControllerAdvice`) — all exceptions are translated to `ApiResponse` here. New domain-specific exceptions should extend the existing pattern (e.g. `BusinessException`, `ResourceNotFoundException`) and get a handler added in `GlobalExceptionHandler`, not be caught ad hoc in controllers.
- `common.model.BaseEntity` — shared entity base (id/audit fields); all JPA entities extend it.

### Auth

Stateless JWT auth: `JwtService` (issue/parse), `JwtAuthenticationFilter` (per-request), `JwtAuthenticationEntryPoint` (401 handling), `CustomUserDetailsService`, wired in `config/SecurityConfig`. `jwt.expiration` is configured in `application.properties`.

### AI agent layer (`ai/`)

This is the most novel part of the codebase — read multiple files here before changing behavior:

- **`ai.llm`** — provider-agnostic chat abstraction. `LlmClient` is the single interface (`chat(LlmRequest) -> LlmResponse`); `LlmRequest`/`LlmResponse` use provider-neutral types (`Message` sealed hierarchy: `SystemMessage`, `UserMessage`, `AssistantMessage`, `ToolMessage`; `ToolSpecification`, `ToolCall`). `ai.llm.gemini` is the only provider currently implemented (MVP scope) and implements `LlmClient`, doing its own request/response mapping to/from these neutral types — provider-specific JSON shapes must never leak outside the `llm.<provider>` subpackage. A future provider would follow the same shape and be selected via `align.llm.provider` + `@ConditionalOnProperty`. Provider selection/config lives in each provider's own `*Properties`/`*Config`.
- **`ai.tool`** — `Tool<T>` is the extension point for anything the agent can invoke (`name()`, `description()`, `parameters()` as a JSON-schema-like map, `execute(ToolContext)`). Every `Tool<?>` Spring bean is auto-discovered by `ToolRegistry` (constructor-injected `List<Tool<?>>`) — to add a new tool, just implement the interface and annotate it as a `@Component`; no registry wiring needed. Tool names must be unique (`ToolRegistry` throws on collision).
- **`ai.agent`** — `AgentServiceImpl` runs the agentic loop: send messages + tool specs to the `LlmClient`, and while the assistant response contains tool calls, execute them via `ToolExecutionService` and feed `ToolMessage` results back, up to `MAX_STEPS` iterations. Tool execution errors are caught and serialized back to the model as a `ToolMessage` (so the LLM can react), not thrown.
- **`ai.agent.execution.ToolExecutionService`** — looks up the tool in `ToolRegistry` by name and executes it with a `ToolContext` (carries the authenticated `User`), decoupling the agent loop from tool dispatch.
- **`ai.dev.ToolDevController`** — `POST /dev/tools/execute`, a debug endpoint that runs a single `ToolCall` through `ToolExecutionService` directly, bypassing the LLM/agent loop entirely. Useful for testing a new `Tool<T>` in isolation.

When adding a new agent capability, prefer adding a new `Tool<T>` implementation over branching inside `AgentServiceImpl`.
