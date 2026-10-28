# 0019 — Testing strategy — unit + Testcontainers

- **Status:** Accepted
- **Date:** 2026-09-08
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Two layers, deliberately kept apart — nothing in between (no `@DataJpaTest`, no `@WebMvcTest` anywhere):

- **Unit tests** (the bulk, 312 as of 2026-09-13) — plain JUnit 5 + Mockito + AssertJ, no Spring context. Every `ServiceImpl`, every `Tool`, every `@Scheduled` job, the thin controllers' delegation, and (since 2026-09-12) `JwtService`, `JwtAuthenticationFilter` (via `MockHttpServletRequest`) and `GlobalExceptionHandler`. Two useful idioms from spring-test that need no context: `ReflectionTestUtils.setField(entity, "id", uuid)` when an unpersisted entity's id is part of the assertion (`BaseEntity.id` has no setter), and `MockMultipartFile` for multipart controllers. First line, stays the default: fast, no infrastructure.
- **Integration tests** (`src/test/java/com/jet/align/integration/`, added 2026-09-08) — `@SpringBootTest` against a real Postgres via **Testcontainers**, one container shared across the whole suite. Their entire reason to exist is the recurring "the build won't catch it" class of bug documented throughout this file — `@Transactional`-override on a `readOnly` class, missing `ON DELETE` rules, UNIQUE/CHECK constraints, native `Specification` predicates, migration validity — none of which a mock or `ddl-auto=validate` can see.

Decisions:

- **Testcontainers, not the old CI service container.** CI used to spin a `postgres:16` service container just so the scaffold `contextLoads` hit a real DB — which only ever worked in CI, never locally. The singleton-container pattern (`support/AbstractIntegrationTest`: static `PostgreSQLContainer`, `.start()` in a static block, `@DynamicPropertySource` wiring the datasource, Ryuk stops it at JVM exit) runs identically on a dev machine (`mvn verify`, needs Docker Desktop) and on the GitHub runner (Docker preinstalled). `ci.yml` lost its `services.postgres` block and `SPRING_PROFILES_ACTIVE: ci`; `application-ci.properties` was deleted — nothing referenced it anymore.
- **Dedicated `test` profile** (`src/test/resources/application-test.properties`, `@ActiveProfiles("test")` on the base class). Carries `align.timezone=UTC` (deterministic date/time assertions, matches the unit-test convention) plus dummy `jwt.secret` / gemini key / VAPID / `align.crypto.secret` — the values the full context needs to boot. **Force-committed** (`git add -f`), exactly like `application-prod.properties`, because `.gitignore` line 23 ignores every `*.properties`; miss the `-f` and CI has no test config and every `@SpringBootTest` fails.
- **Additive, not a rewrite.** The ~232 Mockito tests were untouched. Only `AlignApplicationTests.contextLoads` (Spring Initializr scaffold) was removed, replaced by `SchemaIntegrationTest`.

First three integration tests:

- `SchemaIntegrationTest` — the `@SpringBootTest` boot itself proves every migration applies (20 as of `V20`, 2026-09-13; the assertion is `>= 18`, so no edit needed per migration) and every `@Entity` passes `ddl-auto=validate`. Three extra assertions cover what `validate` ignores: the Flyway history row count, the `ON DELETE CASCADE` on `fk_habit_completions_habit` (the `V11` lesson), and the `uk_user_email` UNIQUE (the `V12` lesson).
- `TaskExpirationIntegrationTest` — runs the real `TaskSpecifications.expirable` predicate against SQL. Proves what the mock can't: undated tasks never expire (SQL `NULL` comparisons), `dueDate = today` only expires past its `dueTime`, completed/future tasks untouched. The same-day-time case derives `dueTime` from `LocalTime.now(UTC)` ±5 min and `assumeTrue`-skips within 5 min of midnight UTC (where `LocalTime` wraparound is non-deterministic) — cheaper than injecting a `Clock` into production.
- `HabitCascadeIntegrationTest` — `habitService.deleteHabit` on a habit with completions, asserts the rows are gone. `Habit` doesn't map the completion collection, so the delete is a bare `DELETE FROM habits` and only the DB-level `ON DELETE CASCADE` (`V11`) saves it — without it this throws `DataIntegrityViolationException`.

Full suite after this: **238 tests, all green** (2026-09-08). As of 2026-09-13 (password reset, uncommitted): **319 tests** — 312 unit green; the 7 integration tests were not run locally (Docker down) and still need a green run against `V20` before merge. `IntegrationTestData` holds the shared `newUser()` builder (random-UUID email to dodge `uk_user_email` across tests sharing the container).



## Later notes

- **2026-10-27 — correction.** `TaskExpirationIntegrationTest` no longer proves "undated tasks never expire". That case was removed when `Task.dueDate` became mandatory (`V17`, see [0020](0020-timed-reminders.md)).
- **2026-10-27.** The "integration tests not run against `V20`" item is resolved. CI (`mvnw verify`, Testcontainers) went green on PR #39, and the 2026-10-24 production deploy succeeded. Suite size at this date: 320 `@Test` methods across 61 files.
- **2026-10-27 — gap.** `CreateTaskTool` and `DeleteTaskTool` have no unit tests, even though "every Tool" is supposed to be tested. `DeleteTaskTool` was the Phase 3 proof case for `PendingAction`.
