# 0006 — Auth — JWT expiration handling and user-id subject

- **Status:** Accepted
- **Date:** 2026-08 / 2026-09-12
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


**2026-09-12 — JWT `subject` = user id (was the email).** Done as the prerequisite for letting the user change their email from the profile (see [User domain](0021-user-domain.md)). Traced before changing anything: with `subject = email`, an email change would leave the live token pointing at a user that no longer exists under that key → `CustomUserDetailsService.loadUserByUsername` throws `UsernameNotFoundException`, which is **not** a `JwtException`, so the filter's `catch` didn't cover it → uncaught outside `DispatcherServlet` → `500`, not `401`. The same 500 was already latent for "user deleted while a token is still alive". Two paths were weighed: (A) re-issue a token from the email-change endpoint and widen the catch — works, but leaves the token's identity mutable and any *other* live token for that user broken; (B, chosen) make the token identity immutable. What changed:

- `JwtService.generateToken` → `.subject(user.getId().toString())`; the redundant `id` claim was dropped, `role` stays. `extractEmail` → `extractUserId(): UUID`. A token whose subject isn't a UUID (i.e. every token issued before this change) throws `MalformedJwtException` — a `JwtException` — so legacy tokens fall into the existing 401 path instead of an `IllegalArgumentException` 500. **Consequence: every session alive at deploy time dies once**; log in again.
- `isTokenValid`/`isTokenExpired` deleted. `isTokenExpired` was already dead (JJWT rejects expired tokens at parse time, as documented below); `isTokenValid` only compared the subject against the username of the user loaded *by that same subject* — a tautology.
- `JwtService` moved from field `@Value` to an explicit constructor (`(String secret, Duration expiration)`) — the established idiom, and what made `JwtServiceTest` possible at all.
- `CustomUserDetailsService` gained `loadUserById(UUID)`. Login still goes through `loadUserByUsername(email)` via `AuthenticationManager`; the filter uses `loadUserById`. Two lookups, one class — "how a user is resolved for security" stays in one place rather than the filter reaching into `UserRepository`.
- `JwtAuthenticationFilter` catches `JwtException | UsernameNotFoundException`, closing the deleted-user 500 for good. Both are different causes with the same client reaction — the same collapse-to-one-401 argument below.
- Tests: `JwtServiceTest` (round-trip, legacy email-subject → `JwtException`, expired → `ExpiredJwtException` at parse, wrong signature) and `JwtAuthenticationFilterTest` (`MockHttpServletRequest`, no Spring context; the case that matters is "valid token, user gone → chain continues, no auth, no exception"). First tests of this layer.

The rest of this section is the 2026-08 expiration fix, still accurate except where the bullets above supersede it (`isTokenValid`'s "dead code" note is now moot — the method is gone).

Not part of the Personal assistant roadmap phases — a hardening pass on existing MVP infrastructure, done as a detour between Phase 3 and Phase 4 after a direct question about what happens when a JWT expires.

Problem found (not hypothetical, traced through the actual code): `JwtService.extractAllClaims` parses the token via `Jwts.parser()...parseSignedClaims(token)`, and JJWT validates expiration **during parsing** — an expired token makes `parseSignedClaims` throw `ExpiredJwtException` before `JwtService.isTokenExpired` is ever reached (that method was effectively dead code for this case). `JwtAuthenticationFilter` didn't catch this. Since servlet filters run before `DispatcherServlet`, the exception never reached `GlobalExceptionHandler` (which only sees exceptions thrown inside controllers) — it propagated as an uncaught exception, and the client got a generic Spring Boot `500`, not a `401`. A second, unrelated fragility in the same method: `filterChain.doFilter(...)` was only called *inside* the `if (email != null && ...)` block, so any path that didn't enter that block would drop the request with no response at all — dormant today (tokens always carry a subject), but fixed while already in the method.

Decisions made:

- **Every authentication failure — missing token, expired token, invalid signature, malformed token — collapses to the same `401` with the same `ApiResponse` shape.** No message differentiates *why* auth failed. There's no refresh-token flow, so the only sane frontend reaction to any of these is identical (clear the stored token, redirect to login) — distinguishing them in the response would be complexity with no consumer.
- **No refresh token was introduced.** `jwt.expiration=24h` plus "your session expired, log in again" is proportional UX for a single-user personal project; a refresh token earns its complexity only if 24h logouts become real daily friction, not preemptively.
- **The fix keeps a single response path for all "not authenticated" outcomes**, rather than having the filter short-circuit invalid/expired tokens with its own response. `JwtAuthenticationFilter` now catches `JwtException` around the parse/validate block, does *not* set the `SecurityContext`, and always falls through to `filterChain.doFilter(...)` — exactly the same path a missing token already took. That un-authenticated request then gets rejected further down by Spring Security's own authorization check, which invokes the new `JwtAuthenticationEntryPoint`. Caught during review: an earlier version of this fix called `response.sendError(...)` directly from the filter's catch block and returned early — technically also a `401`, but bypassing `JwtAuthenticationEntryPoint` entirely, which meant expired/invalid tokens produced Spring Boot's generic default error JSON while a missing token produced the project's `ApiResponse` shape. Two different bodies for what should be one outcome — fixed by removing the early return so both cases converge on the same entry point.

Implemented:

- Done: `JwtAuthenticationEntryPoint` (`auth`) — implements `AuthenticationEntryPoint`, writes `ApiResponse.error(HttpStatus.UNAUTHORIZED, "Authentication required.")` directly to the response, same JSON shape as every other error path in the app. Wired into `SecurityConfig` via `.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))`.
- Done: `JwtAuthenticationFilter` — parse/validate wrapped in `try/catch (JwtException e)`, catch block intentionally empty (falls through), `filterChain.doFilter(...)` moved to always run exactly once regardless of outcome.

Frontend contract (not this repo, but the behavior this backend now guarantees): any `401` response means the token is no longer usable, for any reason — clear it and redirect to login. No response-body inspection needed beyond the status code.


