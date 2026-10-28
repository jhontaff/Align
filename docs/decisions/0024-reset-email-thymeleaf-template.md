# 0024 — Reset email rendered with a Thymeleaf template

- **Status:** Accepted (partially supersedes 0022)
- **Date:** 2026-10-23 (commit `ca44df1`, PR #39)
- **Origin:** reconstructed from code on 2026-10-27. The rationale below still needs confirmation from the author.

## Context

[0022](0022-password-reset.md) decided the reset email body would stay inline HTML in `AuthServiceImpl.buildResetEmail` ("a template engine earns its place with the second email, not the first").

## Decision

- The body moved to `src/main/resources/templates/email/reset-password.html` and is rendered with `spring-boot-starter-thymeleaf`.
- `AuthServiceImpl` now receives `SpringTemplateEngine` in its constructor. It builds a `Context` (locale `es`) with `firstName`, `resetLink` and `expiresInMinutes`, then calls `templateEngine.process("email/reset-password", context)`.
- The template is a table-based 600px card with inline styles, a preheader, and light/dark color-scheme support.
- `AuthServiceImplTest` builds a real `SpringTemplateEngine` over a `ClassLoaderTemplateResolver` (`templates/` prefix), so the test renders the actual template.

## Consequences

- The "wait for the second email" rule from 0022 was reversed at the first email. **The rationale was not recorded and is pending.** A plausible reason: a branded HTML email is hard to keep readable inside a Java string.
- `AuthServiceImpl` (package `auth`) now depends directly on Thymeleaf. `email.EmailSender` stays provider-neutral, but rendering happens in the caller. If a second email ever appears, decide then whether rendering belongs in `email` (for example an `EmailTemplateRenderer`) rather than in each service.
- `spring-boot-starter-thymeleaf` also enables Spring MVC view resolution for `templates/`. Web views don't use it, and nothing else should start rendering pages from it.
