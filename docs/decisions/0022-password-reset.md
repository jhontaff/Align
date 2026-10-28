# 0022 — Password reset by email

- **Status:** Accepted (email body superseded by 0024)
- **Date:** 2026-09-13
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Built 2026-09-13, right after the User domain landed. Until now there was no recovery path at all: a forgotten password meant editing the DB by hand. Email-with-link was chosen over the alternatives without much contest — email is already the identity anchor (registration, login), so it proves ownership through a channel that exists; SMS OTP would need a phone field + a second provider for nothing extra, and security questions are a deprecated pattern. A YAGNI check was run explicitly (single real user, DB access available) and the feature was built anyway as a deliberate call.

Three decisions, all made before writing code:

- **The token is a DB row, not a short-lived JWT with a `purpose` claim.** A signed JWT can't be invalidated after use without DB state anyway (it stays replayable within its expiry window), so "reuse `JwtService`" only looks cheaper. Same "system guarantee, not trust" stance as `PendingAction` in Phase 3. `PasswordResetToken` (`auth`): `user`, `tokenHash`, `expiresAt`, `usedAt` (nullable), extends `BaseEntity`. Two query methods, `isUsed()` and `isExpired(Instant now)` — the latter takes `now` so the service evaluates the whole operation against one instant and tests control time without a `Clock`.
- **The hash is SHA-256, never `PasswordEncoder`.** This one is a correctness issue, not a preference: bcrypt salts differently on every `encode()`, so two hashes of the same value differ and `WHERE token_hash = ?` can never match — you'd have to load every active token and `matches()` each. A deterministic hash is correct here because the input is 32 bytes of `SecureRandom` (base64url, 43 chars), not a human-chosen password — the reason bcrypt is slow doesn't apply. Lives in `auth.impl.PasswordResetTokenGenerator`, package-private, same role and placement as `ApiKeyCipher`. `PasswordResetTokenGeneratorTest` pins the determinism (and the exact SHA-256 of `"abc"`) so nobody "fixes" it to bcrypt for consistency.
- **One active token per user, enforced by the DB.** `V20__add_password_reset_tokens.sql` declares a **partial unique index** `(user_id) WHERE used_at IS NULL` — same shape as `calendar_events`' partial reminder index, same "the base guarantees it, not the service" rule as `uk_user_email`/`uk_llm_credentials_user`. The service still deletes the previous unused token before inserting (`findByUserAndUsedAtIsNull` → `delete`), so a second request replaces the first instead of hitting the index. Also in `V20`: `UNIQUE(token_hash)` (doubles as the lookup index), FK to `users` with `ON DELETE CASCADE` declared in SQL, `token_hash VARCHAR(64)` (SHA-256 hex is exactly 64).

Anti-enumeration, applied twice:

- `POST /auth/forgot-password` returns **200 with the same message whether or not the email exists** ("Si el correo está registrado, vas a recibir un enlace…"). Unknown email → the service just returns.
- **A failed email send is caught and logged, never propagated.** A 500 only when the email *is* registered would be an existence oracle. The token is already persisted; the user can request again. This is why `ResendEmailSender` throws its own `EmailDeliveryException` (`common.exception`, extends `RuntimeException`, **not** `BusinessException`): a provider outage is infrastructure, not a bad request — if it ever did propagate, the catch-all's 500 is the honest status. No dedicated handler.
- `POST /auth/reset-password`: "not found", "already used" and "expired" all throw the same `BusinessException("El enlace no es válido o ya expiró.")` — distinguishing them helps nobody legitimate.

Other decisions:

- **`email` is a new top-level package, sibling of `ai`/`common`/`scheduler` — not inside `notification`.** That domain owns Web Push and has its own entity/subscription lifecycle; email is a different transport, and mixing them would be the "one big service" smell. `email.EmailSender` is a one-method neutral interface (`send(to, subject, htmlBody)`); `email.resend.*` (`ResendProperties` `@ConfigurationProperties("align.email")`, `ResendConfig`, package-private `ResendApi` wire DTOs, `ResendEmailSender`) mirrors `ai.llm.gemini` exactly. `AuthServiceImpl` depends on `EmailSender` only. One difference from Gemini, on purpose: Resend has a single key, no pool, so `Authorization: Bearer` is a `defaultHeader` on the `RestClient` rather than per-request.
- `ResetPasswordRequest` reuses `@ValidPassword` and the same `@AssertTrue isPasswordConfirmed()` as `RegisterRequest`/`PasswordUpdateRequest` — third consumer of the composed constraint, zero copies of the rule.
- Both endpoints are under `/auth/**`, already `permitAll` — `SecurityConfig` untouched.
- **Resetting the password does not invalidate sessions** — the same rule as `changePassword` (JWT independent of the password, no revocation list), not a new decision.
- `AuthServiceImpl` moved to an explicit constructor (two `@Value`s: `align.password-reset.ttl-minutes=30`, `align.frontend.base-url`) — the established idiom. It **keeps** `jakarta.transaction.Transactional` without `readOnly`; that outlier is documented above and fixing it wasn't this feature's job.
- TTL is 30 min and checked at use time; **no cleanup job** — used/expired rows are inert and single-user volume doesn't justify a sweep. (Recorded as a non-candidate for `scheduler`, unlike `PendingActionExpirationJob`, which changes behaviour.)
- The link is `<align.frontend.base-url>/reset-password?token=<raw>` — the first place the backend needs the frontend's public URL (push deep-links are relative because they're same-origin; an email client isn't). Hence the new config value.

Config: `align.email.base-url=https://api.resend.com` in `application.properties`; `align.email.api-key`, `align.email.from` (must be a sender verified in the Resend account), `align.frontend.base-url` per profile — dev in the gitignored `application-dev.properties` (`onboarding@resend.dev` is Resend's no-domain test sender, delivers only to the account owner), test dummies, prod via **`ALIGN_RESEND_API_KEY` / `ALIGN_EMAIL_FROM` / `ALIGN_FRONTEND_BASE_URL`**.

Tests (20 new, plain JUnit 5 + Mockito, no Spring context): `PasswordResetTokenGeneratorTest` (5), `ResendEmailSenderTest` (3, `MockRestServiceServer` like `GeminiLlmClientTest`), `ResetPasswordRequestTest` (4, `Validator`-based), and **`AuthServiceImplTest` (8) — the first unit test of `AuthServiceImpl`**, covering only the reset flow; `register`/`login` remain untested (pre-existing gap). It uses the *real* generator instead of a mock so it proves end-to-end that the raw token in the email body is the one later found by hash.

Frontend contract (not this repo — written so it can be copied verbatim into `align-web`'s `CLAUDE.md`):

- Both endpoints are public: **no `Authorization` header**. Every response uses the standard envelope `{ timestamp, status, success, message, data, errors }`.
- **`POST /auth/forgot-password`** — body `{ "email": string }`. Always **`200`** with `data: null` and the message *"Si el correo está registrado, vas a recibir un enlace para restablecer tu contraseña."* — the same whether the email exists or not, and even if the email failed to send. Show that message as-is; never say "email sent" or "email not found". Only non-200: `400` for a blank/malformed email (`errors: { "email": "..." }`).
- The email carries a link to **`<frontend>/reset-password?token=<raw>`**. That route must read `token` from the query string and hold it for the form; the token is 43 URL-safe chars, opaque, never decode or validate it client-side. The link expires **30 minutes** after the request.
- **`POST /auth/reset-password`** — body `{ "token": string, "newPassword": string, "confirmPassword": string }`. Success is **`200` with `data: null`** — **it does not return a JWT and does not log the user in**. After success, redirect to login with a "contraseña actualizada" notice; the user signs in with the new password.
- Errors on `reset-password`:
  - `400` with `errors` map = validation. Keys: `token` (blank), `newPassword` (policy: 8–25 chars, at least one lowercase, one uppercase, one digit — same rule as register and profile password change), `confirmPassword` (blank), and **`passwordConfirmed`** when the two don't match — note that key is the name of the `@AssertTrue` method, not a field; map it to the confirm input.
  - `400` with `errors: null` and message *"El enlace no es válido o ya expiró."* = the token is unknown, already used, or expired — deliberately indistinguishable. Don't retry; offer "pedir un enlace nuevo" (back to forgot-password).
- Requesting a new link **replaces** the previous one: only the most recent link works. No rate limit on `forgot-password`, but don't auto-resend.
- Existing sessions on other devices are **not** invalidated by a reset (same rule as changing the password from the profile).

Open at the time of writing:

- **The three env vars above must be added to `/etc/align/align.env` on the VPS before the next deploy** — unresolved `${...}` placeholders fail startup.
- **Integration layer not run against `V20`** — Docker down again. `SchemaIntegrationTest` must go green before merge.
- Frontend: a `/reset-password` route that reads `?token=` and calls the endpoint. Not built.
- No rate limiting on `forgot-password` — each call costs one Resend send for a registered email. Same stance as `PUT /api/ai/credentials`: fine at this scale.
- The email body is inline HTML in `AuthServiceImpl.buildResetEmail`. A template engine earns its place with the second email, not the first.



## Later notes

- **2026-10-27.** Committed to `main` on 2026-10-23 and deployed on 2026-10-24 (PR #39). Status of the "Open" items above:
  - Env vars on the VPS: resolved. The deploy health check passed, and the app cannot start without them.
  - Integration layer against `V20`: resolved (CI green).
  - Inline HTML email body: superseded. The body is now a Thymeleaf template; see [0024](0024-reset-email-thymeleaf-template.md).
  - Frontend `/reset-password` route: can't be checked from this repo.
- **2026-10-27 — gap fixed.** `deploy/align.env.example` was missing the three new variables. They were added in the same cleanup.
