# 0011 — Schema reconciliation — migration V12

- **Status:** Accepted
- **Date:** 2026-09-01
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Not a feature — a hardening pass on the schema, done 2026-09-01 while auditing the migrations after a local DB got out of sync. Trigger: a review of `V1`–`V11` against the JPA entities turned up things every entity already *assumes* but no migration ever enforced. None of them broke startup, because `ddl-auto=validate` only checks column existence and types — it ignores unique constraints, `ON DELETE` rules, check constraints, nullability, and indexes (the same blind spot behind the `V11` FK-cascade fix). `V12__reconcile_schema.sql` closes the gaps that mattered:

- **`uk_user_email` UNIQUE on `users(email)`** — `User` declares `@UniqueConstraint(name = "uk_user_email")`, but `V2` only created a non-unique `CREATE INDEX idx_user_email`. Until `V12`, email uniqueness was enforced **only** by `AuthServiceImpl`'s `existsByEmail()` check — two concurrent registrations with the same email could both pass it. The old non-unique `idx_user_email` is left in place (the entity still declares the matching `@Index`); it's redundant next to the unique index now, harmless.
- **`tasks` and `conversation_histories`: `created_at` / `updated_at` → `timestamptz NOT NULL`** — `BaseEntity` maps both as `Instant` (UTC) with `nullable = false`, and every table from `V2` on uses `TIMESTAMP WITH TIME ZONE NOT NULL`. Only `V1` (`tasks`) and `V4` (`conversation_histories`) predated that convention and used bare `TIMESTAMP` nullable. The `ALTER ... TYPE timestamptz USING <col> AT TIME ZONE 'UTC'` reinterprets the stored wall-clock as UTC, which is how Hibernate writes an `Instant` into a zoneless column — so existing rows keep the right instant.
- **`DROP TABLE IF EXISTS example`** — the Spring Initializr scaffold table from `V1`, never mapped by any entity.

Deliberately **not** in `V12`:

- **`tasks.description NOT NULL`** — `Task.description` is `@Column(nullable = false)` and the service always sets it, but `V1` created the column nullable. Left nullable for now (user's call, 2026-09-01) — a known, tolerated entity/DB mismatch, not an oversight. `validate` doesn't catch it, so it costs nothing until someone decides to tighten it.
- **Dropping the `tool_call_id` / `conversation_snapshot_json` phantom columns** that showed up on one local `pending_actions` table — those were never in any migration or entity on any branch (leftovers from a Phase 3 experiment run under `ddl-auto=update`). A `DROP COLUMN` for them would fail on a fresh clone where they don't exist. That's local-DB drift, fixed by hand on the affected machine (or a full `DROP SCHEMA public CASCADE` + Flyway rebuild), never as a shared migration.

`V12` is written to run on both an empty schema (fresh clone) and a populated one. On a populated DB its `ALTER`s have preconditions — no duplicate emails, no null `created_at`/`updated_at` in the two tables — trivially met if the data came through the app; check them first if in doubt, since a failing statement rolls the whole migration back (Postgres runs each migration in a transaction).


