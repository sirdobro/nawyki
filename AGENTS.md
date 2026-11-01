# Repository instructions

## Purpose and scope

Build the semester project described in README.md and docs/. Read these documents before implementation. The main product is a habit tracker. Keep the required strength-training journal minimal: date, manually named exercises, individual sets with repetitions and optional weight in kg, history, edit and delete. No training plans, exercise library, timers, personal records, wearables or automatic linkage between a workout and a habit.

User-facing product text and project documentation default to Polish. Code identifiers default to English. Respond to the user in their current language.

## Fixed technology decisions

- Java 21, Spring Boot, Spring MVC functional routing, PostgreSQL, JDBC and explicit SQL.
- React with JavaScript/JSX and CSS; Vite for frontend builds. Do not introduce TypeScript without agreement.
- No WebFlux/Reactor, R2DBC, Vavr, Hibernate/JPA, Next.js, Redux or additional infrastructure without a concrete need and user agreement.
- Prefer Spring JDBC helpers over repetitive connection-management code. Keep SQL and record mapping visible.
- Maven Wrapper for reproducible Java builds. Pin compatible library and frontend dependency versions and commit applicable lockfiles and wrapper files.
- Keep ordinary project source, configurations and dependency lockfiles tracked; do not confuse configuration with secrets.

## Functional architecture

- Keep the domain a plain Java module/package without Spring, HTTP, persistence or frontend dependencies.
- Model products with records and variants with sealed types. Defensively copy collections and ensure nested values are immutable; records alone do not guarantee this.
- No null or exceptions as normal control flow in the domain. Use Optional for missing values and explicit Result/error variants for failures.
- At least one validator must accumulate multiple independent errors.
- Pass the current date, zone, identifiers and other dependencies as values/functions. No hidden clock, randomness, logging or database calls inside pure functions.
- Keep effects at application boundaries. Translate infrastructure failures at the boundary and map domain errors to documented HTTP responses.
- Compose routes and filters as functions. Provide at least eight JSON REST endpoints and two genuine server-side SSE endpoints.
- Use JDBC transactions for atomic multi-step writes. Do not share mutable state or JDBC connections across concurrent tasks.

## Product rules

Follow docs/product.md. Daily habits and selected-weekday habits have scheduled-day streaks. N-days-per-week habits have weekly streaks. Do not count unfinished current periods as failures. Dates follow the configured user time zone, initially Europe/Warsaw. Prevent duplicate habit/date completions in the database as well as in application logic.

The current product is online-first; do not add offline synchronization. Approved access model: multiple pseudonymous accounts without names, emails or passwords. Use a cryptographically random access key, manual entry and an optional private bookmark with a URL fragment; exchange it for an independent HttpOnly session cookie and remove the fragment immediately. Store only key verifiers/hashes. Scope every REST operation and SSE subscription to its authenticated account. See docs/authentication.md.

## Security

Read SECURITY.md. Never commit credentials, tokens, private keys, live environment files, database files, dumps, user data or private course documents. Keep tracked `.env.example` values blank or obviously non-secret.

Before each commit review staged content and verify ignore patterns. Do not print secrets while inspecting files. If a secret is already tracked, ignoring it is insufficient: remove it from tracking and arrange revocation/rotation; do not rewrite published history without authorization.

## Tests and evidence

- Implement unit tests for the domain, at least three meaningful property-based tests and integration tests against PostgreSQL.
- Require at least 70% domain line coverage and retain a report suitable for documentation.
- Test scheduling, streak boundaries, duplicate requests, transaction rollback, concurrent duplicate completion, and both SSE endpoints.
- Use a disposable PostgreSQL database in CI, isolated from the persistent online database. Never run destructive tests against deployed data.
- Keep achieved results separate from planned work. Do not invent coverage, test counts or implemented features.

## Development and deployment

The selected repository is https://github.com/sirdobro/nawyki.git and the intended local checkout is C:\Users\mikep\Documents\Prog\nawyki.

Target one provider/VPS for the complete application and PostgreSQL. The user purchased OVHcloud VPS-1 running Ubuntu 26.04 LTS on x86_64, administered as ubuntu using a dedicated SSH key. Read docs/vps-security.md and docs/hosting-setup.md before server changes. The target hostname is nawyki.intmain.cc, with DNS managed at Cloudflare. Serve the built React files through Spring; Node is needed for frontend builds, not as a separate production server.

The desired workflow is GitHub Actions build/test followed by deployment and online manual verification. The VPS runtime (Docker, private PostgreSQL, Caddy) is installed. DNS and public HTTPS for nawyki.intmain.cc were verified on 2026-10-31; Cloudflare proxy is enabled and independently verified (HTTPS 200, Server: cloudflare, dynamic response). The foundation Java/React application and CI are implemented and first deployed as df4b2f2. Accounts and habits are implemented and deployed as f2ab4c9, with 25 passing tests and public browser/API verification recorded in docs/verification.md; current deployment must still be checked in /api/status. Workouts, statistics and SSE remain to be built. GitHub Actions verifies and publishes a public release image; the VPS pulls it without GitHub credentials or an SSH key on GitHub. No credentials, workflow executions, passing checks or live URLs may be assumed.

Use short-lived branches and reviewable changes. Do not push, provision paid resources, deploy, publish or modify account settings unless authorized. A request for a local commit does not authorize a push.

Do not generate pretend deployment workflows that silently skip required tests. Add executable CI when the application actually builds. Keep docs current as features are implemented, and maintain the course requirements matrix and accurate AI-use record.

## Project work log

For this nawyki project only, maintain C:\Users\mikep\Documents\Studia\projekty\work_log.md after important completed steps. The user explicitly requested ongoing updates. This is not a global preference for unrelated projects.

Record concise chronological entries prefixed `Human:` or `Agent:` with dates where known. Include significant decisions actually approved, implementation milestones, verified tests, commits/pushes, server changes and user-completed account setup. Do not copy the conversation, routine tool calls or unexecuted plans. Never include credentials, access keys, private SSH keys or personal journal data. Preserve existing history and keep the current checkpoint accurate. Distinguish user-reported actions from independently verified results. If access is unavailable, request the narrowly scoped filesystem permission and report any unrecorded updates honestly.

Do not log side questions or explanation sessions. Keep the log focused on significant project actions and results.

## Keep the stack explainable

The user explicitly asked to avoid unexplained component additions. The user approves industry-standard supporting tools and helpers provided their purpose is explained and documented. Keep docs/tools.md accurate; use concrete needs to limit unnecessary dependencies. This does not reopen the fixed application-stack decisions. Caddy is the already agreed HTTPS proxy; Flyway is the SQL-migration helper explained during the foundation work. Use the approved pull-based deployment: GitHub publishes verified public release images, the VPS pulls them. Never upload an SSH private key to GitHub for this project.
