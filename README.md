# Business Review Platform — Backend (DAO layer)

Spring Boot 3.5 / Java 21 / PostgreSQL 16 (+ PostGIS, pg_trgm, fuzzystrmatch).
Package-per-section, matching the consolidated spec (v5) sections 1–16.
"DAO layer" here = JPA entities + Spring Data repositories per package —
one package per spec section, self-contained (own entities, own enums, own repo).

## Package map (src/main/java/com/bdreview/platform/...)

| Package        | Spec section(s)                          | Contents |
|----------------|-------------------------------------------|----------|
| `business`     | §1 Listing CRUD, §3 Search/Filter         | City, Area, Category, BusinessAttribute, Business (PostGIS point), BusinessRepository (search/duplicate-check/atomic-aggregate/soft-delete) |
| `claim`        | §2 Claim flow                             | BusinessClaim + repo |
| `auth`         | §5 Role-based auth                        | User, RefreshToken (rotation + reuse-detection) + repos |
| `otp`          | §6 Phone/OTP verification                 | OtpVerification + repo (rate-limit counting queries) |
| `review`       | §4 Reviews/votes, §7 dashboard queries    | Review, ReviewPhoto, ReviewVote + repos (72h edit window, atomic vote/aggregate updates, N+1-safe rating trend) |
| `verification` | §8 NID verification                       | NidVerification + repo (admin queue, resolve, purge scheduling) |
| `bookmark`     | §9 Favorites/Collections                  | Collection, Bookmark + repos |
| `report`       | §11 Report button                         | Report + repo (rate-limit query) |
| `moderation`   | §12 Admin moderation dashboard             | AuditLog + repo |
| `gallery`      | §13 Multi-photo gallery                    | BusinessPhoto + repo (metadata only — uploads go direct-to-object-storage) |
| `fakereview`   | §14 Fake-review detection signals          | FakeReviewSignal + repo (per-signal breakdown behind Review.suspicionScore) |
| `summary`      | §15 AI review summary                      | BusinessReviewSummary + repo (pessimistic-lock guard against duplicate regeneration) |
| `messaging`    | §16 Consumer→owner direct inquiry          | MessageThread, Message + repos (shared with §7 owner-reply channel) |
| `common`       | cross-cutting                              | PageRequestDefaults (20/30 pagination clamp used across all listing queries) |

`§10 Share Button` and `§17 Map/Location UX` have no DAO surface of their own:
share is a stateless slug→deep-link build on the frontend, and the map's
owner-side pin write / consumer-side static-preview both reuse `Business.location`.

## Schema

`src/main/resources/db/migration/V1__init.sql` — single Flyway baseline covering
every table above, including:
- `postgis`, `pg_trgm`, `fuzzystrmatch` extensions
- GiST spatial index on `business.location`, GiST trigram index on `business.name`
- Partial unique index on `business.slug WHERE deleted_at IS NULL` (soft-deleted
  slugs never block reuse)
- `business.rating_sum` — a column not mentioned in the spec's feature prose, added
  so the average-rating recompute (§1) can be a single atomic `UPDATE` rather than
  read-modify-write, without touching the optimistic-lock `version` column

## API surface (controller + service + repository, per package)

Every package now has the full stack: `Repository` (DAO) → `Service` (business
rules, transactions) → `Controller` (`/api/v1/...`, JWT-authenticated except
where noted). Base path is `/api/v1` unless shown otherwise.

| Package        | Base path                              | Auth |
|----------------|------------------------------------------|------|
| `otp`          | `/otp/request`, `/otp/verify`            | public |
| `auth`         | `/auth/refresh`, `/auth/logout`          | refresh public, logout needs access token |
| `business`     | `/businesses`, `/cities`, `/areas`, `/categories`, `/attributes` | search/browse public, write needs BUSINESS_OWNER |
| `claim`        | `/claims`                                | consumer/owner file; `/claims/queue`+resolve need ADMIN |
| `review`       | `/reviews`                               | submit/edit/delete/vote need auth (OTP-verified account) |
| `verification` | `/nid-verifications`                     | submit needs owner; queue/resolve need ADMIN |
| `bookmark`     | `/bookmarks`, `/collections`             | auth required |
| `report`       | `/reports`                               | create needs auth; queue/resolve need ADMIN |
| `moderation`   | `/admin/moderation/*`                    | ADMIN only |
| `gallery`      | `/businesses/{id}/photos`                | owner-only writes, public read |
| `fakereview`   | `/admin/fake-review-signals/*`           | ADMIN only (analysis itself runs automatically, async, from `review`) |
| `summary`      | `/businesses/{id}/summary`               | read public, regenerate manual-trigger open (rate-limited by the "10 new reviews" check) |
| `messaging`    | `/messages`                              | auth required, participant-only |

Auth model: `POST /otp/verify` returns a `TokenPairDto` (JWT access token +
opaque refresh token). Send `Authorization: Bearer <accessToken>` on
subsequent requests. `SecurityConfig` (auth package) wires the stateless JWT
filter chain; `CurrentUser` (common package) is the read-side helper every
controller uses to get the caller's id/role out of the security context.



These two features are NOT implemented in Java — they call out to a separate
Python **LangGraph/FastAPI microservice** at `../ml-service` (sibling project,
run independently on its own port). On the Java side:

| Package     | Added for ML integration | Purpose |
|-------------|---------------------------|---------|
| `fakereview`| `FakeReviewMlClient` (WebClient), DTOs, `FakeReviewAnalysisService` | Calls `/fake-review/analyze`, persists the per-signal breakdown + verdict, and — critically — retracts/re-applies that review's contribution to the business's rating aggregate via `BusinessRepository.applyRatingAggregateDelta` if the verdict crosses in/out of `RECOMMENDED` |
| `summary`   | `SummaryMlClient` (WebClient), DTOs, `SummaryGenerationService` | Calls `/review-summary/generate` only once ≥10 new reviews have landed since the cached summary's `generatedAt`, using the existing pessimistic-lock repo method so a burst of qualifying reviews triggers at most one regeneration |

`app.ml-service.base-url` in `application.yml` points at this service
(`http://localhost:8081` by default — see `ML_SERVICE_BASE_URL` env var).
`spring-boot-starter-webflux` was added to `pom.xml` purely for `WebClient`;
the rest of the app still uses Spring MVC.



- **Soft delete** is implemented as `deleted_at IS NULL` filters baked into
  repository method names/queries — services must always call the
  `*AndDeletedAtIsNull` variants, never the raw `findById`, for user-facing reads.
- **Rating aggregates** are updated only via `BusinessRepository.applyRatingAggregateDelta`
  and `ReviewRepository.adjust*Count` — both single atomic `UPDATE` statements.
  Never read a `Business`/`Review`, mutate the counter in Java, and save it back.
- **Refresh token reuse detection**: `RefreshTokenRepository.findReusedToken` +
  `revokeFamily` implement the theft-response described in §5 — the service layer
  calls `findReusedToken` first on every refresh attempt, before trusting the token.
- **Fake-review visibility** (§14) is a derived three-state enum
  (`RECOMMENDED` / `NOT_RECOMMENDED` / `HIDDEN`), not a boolean — all
  review-listing queries filter by it explicitly rather than a hidden flag.
- Package names avoid depending on each other's internals; cross-package references
  are by raw `UUID` foreign key (e.g. `Review.businessId`), not JPA `@ManyToOne`
  entity references — this keeps each section's package independently buildable/testable,
  matching the "alada alada package" (separate package per section) request.
