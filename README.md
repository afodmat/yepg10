# YEP – Youth Empowerment Project

A digital empowerment and Virtual Exchange platform for young people (18–35) in Uganda and East Africa:
learning, Virtual Exchange sessions, teams, Impact Projects, mentorship, messaging, assessments, achievements and verifiable certificates.

```
React + TypeScript + Vite + Tailwind  →  REST /api  →  Spring Boot 3.5 (Java 25)  →  Spring Data JPA / Hibernate  →  PostgreSQL (Flyway)
```

## Status – please read
All nine build phases are implemented in code. **The backend has not been compiled or run by its author** (the build environment had no Maven, JDK 25 or PostgreSQL). The frontend *has* been type-checked, unit-tested (10 tests) and production-built. Expect to fix a few compile or runtime errors on first `mvn verify`; `AuthFlowIT` and `LearningFlowIT` exercise the main journeys against a real PostgreSQL and are the fastest way to find them.

## Features
| Area | What exists |
|---|---|
| Accounts | Register (always PARTICIPANT), email activation, login/logout, rotating refresh tokens, forgot/reset/change password, lockout after 5 failures, profile + photo, notification preferences |
| Learning | Activities (search, filter by category/level, pagination), enrol/cancel, lessons → progress %, MCQ assessments (auto-scored, attempts, pass mark), completion |
| Virtual Exchange | Sessions with prompts/resources, registration, upcoming/completed, countries represented, meeting link only for registered users |
| Teams | Create/join (open or invite-only), invitations by email, leave (leadership transfers), remove, member titles, max size from settings |
| Impact Projects | Problem/solution, 6 default milestones, progress from milestones, updates, mentor/facilitator feedback, attachments, status review |
| Mentorship | Mentor directory/profiles, requests, assignments (participant or team) by staff, mentee progress view |
| Messaging | Persistent direct + team conversations, unread counts, polling (service layer is transport-agnostic so STOMP can be added) |
| Recognition | 6 achievements awarded automatically from activity; PDF certificates (OpenPDF) with unique verification code; public verify page `/#verify/CODE` |
| Notifications | Persistent, read/unread, per-type preferences, admin broadcast |
| Admin | Stats, user management (search/filter/sort/paginate, role change, deactivate, create), activities, sessions, teams, projects, mentors, certificates (revoke), achievements, audit log, settings |
| Security | See below |

## Requirements
JDK 25, Maven 3.9+, Node 22+, and a Supabase PostgreSQL project.

## Setup
1. In the Supabase dashboard, copy the project's database connection details. For networks without IPv6 support, use the **Session pooler** connection (port `5432`); its JDBC URL has the form `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require` and its username is `postgres.<project-ref>`. The direct database host may require IPv6.
2. Set `DB_URL`, `DB_USER` (or `DB_USERNAME`), `DB_PASSWORD`, and a strong `JWT_SECRET` of at least 32 bytes (see `.env.example`). Spring does not load `.env` automatically; export the variables or load them in your IDE/PowerShell run configuration.
3. Backend: `cd backend && mvn spring-boot:run` – Flyway creates the schema; the `dev` profile seeds demo accounts and sample content.
4. Frontend: `cd frontend && npm install && npm run dev` → http://localhost:5173 (Vite proxies `/api` to :8080 so cookies are same-origin).

Demo accounts (dev only, password = `YEP_DEV_SEED_PASSWORD`): `admin@`, `facilitator@`, `mentor@`, `amina@` (participant, leads *Bright Futures*), `peter@`, `brian@`, `wanjiru@` – all `@yep.local`.
Registration emails are written to the backend log in dev (`LoggingMailService`).

## Configuration
| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USER` (or `DB_USERNAME`), `DB_PASSWORD` | Supabase PostgreSQL connection |
| `JWT_SECRET` | HMAC key for access tokens (required, ≥32 bytes) |
| `COOKIE_SECURE` | `true` in production (HTTPS), `false` for plain-http local dev |
| `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, `API_URL` | Origins and link targets |
| `STORAGE_PATH` | Upload directory |
| `FORWARD_HEADERS_STRATEGY` | `none` (default) or `native` when behind a trusted reverse proxy, so the rate limiter sees real client IPs |
| `SPRING_PROFILES_ACTIVE` | `dev` enables seed data + debug logging; never use in production |

## Roles
| Role | Can |
|---|---|
| PARTICIPANT | Own profile; enrol; lessons/assessments; register for sessions; create/join a team; run the team's project; message mentors/teammates; request mentors; certificates |
| MENTOR | Mentor profile; accept requests; see assigned mentees/teams; comment on their projects; message mentees |
| FACILITATOR | Create/manage *their own* activities, assessments and sessions; see rosters/results; review projects; manage teams; assign mentors |
| ADMIN | Everything above on all content + users, roles, certificates, achievements, settings, audit log, broadcasts |
Roles are enforced on the server (URL rules + `@PreAuthorize` + per-resource ownership checks). The client never supplies a role or user id.

## API overview
`/api/auth/*` register · activate · login · refresh · logout · forgot-password · reset-password  
`/api/users/me` (+`/profile`, `/password`, `/photo`) · `/api/dashboard` · `/api/public/stats`  
`/api/activities` (+`/{id}/enroll`, `/lessons/complete`, `/assessment`, `/enrollments`) · `/api/enrollments/mine` · `/api/assessments/{id}/submit|attempts/mine|results`  
`/api/exchange-sessions` (+`/{id}/register`) · `/api/teams` (+join, members, invitations) · `/api/invitations/*`  
`/api/projects` (+updates, comments, milestones, attachments, status) · `/api/mentors` · `/api/mentorship/*`  
`/api/conversations` (+messages, direct, read, unread-count) · `/api/notifications` · `/api/achievements` · `/api/certificates` (+`/{id}/pdf`, public `/verify/{code}`)  
`/api/files` · `/api/directory/participants` (staff) · `/api/admin/*` (stats, users, audit-logs, settings, notifications, achievements, certificates)  
Errors: `{timestamp,status,error,message,path[,fieldErrors]}`. Lists are paginated (`page`,`size`, max 50).

## Security notes
BCrypt via Spring's delegating encoder · JWT access token (15 min) + rotating hashed refresh tokens (14 d) in **httpOnly SameSite cookies** · CSRF double-submit cookie enabled · per-IP rate limit and per-account lockout on auth · no account enumeration · bean validation on every DTO · parameterised queries only (sort fields whitelisted) · uploads: size/type/extension/magic-byte checks, executable names blocked, images re-encoded, random server-side names, access checked on download · audit log for security and admin actions · no secrets in source · stack traces never returned.
Known limits: an access token stays valid up to 15 min after a user is disabled; the rate limiter is in-memory (single instance); messaging uses polling; assessments are multiple-choice only (no manual grading).

## Testing
Backend: `cd backend && mvn test` runs the regular test suite without Docker. The optional `*IT` integration tests use Testcontainers and require Docker when running `mvn verify`; Docker is not required to run the app, which connects to Supabase.  
Frontend: `cd frontend && npm run typecheck && npm test && npm run build`.

## Deployment
Build `mvn -DskipTests package` → run the jar with production env vars (`COOKIE_SECURE=true`, strong `JWT_SECRET`, real SMTP implementation of `MailService`, no `dev` profile). Serve `frontend/dist` behind the same origin as `/api` (reverse proxy) so cookies stay first-party. Move uploads to object storage by implementing `StorageService`.

## Responsible AI
No AI features are included. Any future AI assistance must be labelled, keep humans in charge of eligibility, certification and discipline, and protect user data.
