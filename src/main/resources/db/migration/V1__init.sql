-- =====================================================================
-- V1__init.sql
-- Verification-Centric Local Business Review Platform (Bangladesh)
-- Baseline schema. Multi-city-ready data model; Dhaka-only business rules
-- are enforced in application code, not in the schema.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- section: business (City / Area / Category / BusinessAttribute / Business)
-- ---------------------------------------------------------------------
CREATE TABLE city (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE area (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id     UUID NOT NULL REFERENCES city(id),
    name        VARCHAR(100) NOT NULL,
    UNIQUE (city_id, name)
);

CREATE TABLE category (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE business_attribute (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE  -- parking, wifi, delivery, home_service, wheelchair_accessible ...
);

CREATE TABLE business (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id     UUID NOT NULL,
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(280) NOT NULL,
    category_id       UUID NOT NULL REFERENCES category(id),
    city_id           UUID NOT NULL REFERENCES city(id),
    area_id           UUID NOT NULL REFERENCES area(id),
    contact_number    VARCHAR(20) NOT NULL,          -- E.164
    operating_hours   TEXT,
    description       TEXT,
    cover_photo_url   TEXT,
    location          geography(Point, 4326) NOT NULL,
    price_tier        VARCHAR(20) NOT NULL DEFAULT 'MODERATE'
                          CHECK (price_tier IN ('BUDGET','MODERATE','EXPENSIVE','VERY_EXPENSIVE')),
    verified          BOOLEAN NOT NULL DEFAULT FALSE,
    average_rating    NUMERIC(3,2) NOT NULL DEFAULT 0.00,
    review_count      INTEGER NOT NULL DEFAULT 0,
    rating_sum        INTEGER NOT NULL DEFAULT 0,   -- backs atomic average_rating recompute; not itself displayed
    deleted_at        TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 0,       -- optimistic lock; never touched by aggregate increments
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Slug uniqueness only among live (non-soft-deleted) rows so a released slug is reclaimable.
CREATE UNIQUE INDEX ux_business_slug_live ON business(slug) WHERE deleted_at IS NULL;

CREATE INDEX ix_business_category   ON business(category_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_business_area       ON business(area_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_business_price_tier ON business(price_tier) WHERE deleted_at IS NULL;
CREATE INDEX ix_business_location   ON business USING GIST(location);
CREATE INDEX ix_business_name_trgm  ON business USING GIST(name gist_trgm_ops);

CREATE TABLE business_attribute_assignment (
    business_id   UUID NOT NULL REFERENCES business(id),
    attribute_id  UUID NOT NULL REFERENCES business_attribute(id),
    PRIMARY KEY (business_id, attribute_id)
);

-- ---------------------------------------------------------------------
-- section: claim
-- ---------------------------------------------------------------------
CREATE TABLE business_claim (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id           UUID NOT NULL REFERENCES business(id),
    claimant_user_id      UUID NOT NULL,
    verification_method   VARCHAR(20) NOT NULL CHECK (verification_method IN ('PHONE','NID')),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at           TIMESTAMPTZ
);
CREATE INDEX ix_claim_business ON business_claim(business_id);
CREATE INDEX ix_claim_status   ON business_claim(status);

-- ---------------------------------------------------------------------
-- section: auth
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number    VARCHAR(20) NOT NULL UNIQUE,   -- E.164, one account/one role
    role            VARCHAR(20) NOT NULL CHECK (role IN ('CONSUMER','BUSINESS_OWNER')),
    otp_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_token (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES app_user(id),
    family_id       UUID NOT NULL,          -- shared by all tokens descended from one login
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    rotated_at      TIMESTAMPTZ,            -- set when superseded by a newer token
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_token_family ON refresh_token(family_id);
CREATE INDEX ix_refresh_token_user   ON refresh_token(user_id);

-- ---------------------------------------------------------------------
-- section: otp
-- ---------------------------------------------------------------------
CREATE TABLE otp_verification (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number    VARCHAR(20) NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    attempts        INTEGER NOT NULL DEFAULT 0,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_otp_phone_created ON otp_verification(phone_number, created_at);

-- ---------------------------------------------------------------------
-- section: review
-- ---------------------------------------------------------------------
CREATE TABLE review (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id         UUID NOT NULL REFERENCES business(id),
    user_id             UUID NOT NULL REFERENCES app_user(id),
    rating              SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content             TEXT,
    visibility_status   VARCHAR(20) NOT NULL DEFAULT 'RECOMMENDED'
                             CHECK (visibility_status IN ('RECOMMENDED','NOT_RECOMMENDED','HIDDEN')),
    suspicion_score     SMALLINT NOT NULL DEFAULT 0,
    useful_count        INTEGER NOT NULL DEFAULT 0,
    funny_count         INTEGER NOT NULL DEFAULT 0,
    cool_count          INTEGER NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMPTZ,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_review_business ON review(business_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_review_user     ON review(user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_review_visibility ON review(business_id, visibility_status) WHERE deleted_at IS NULL;

CREATE TABLE review_photo (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id   UUID NOT NULL REFERENCES review(id),
    url         TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_review_photo_review ON review_photo(review_id);

CREATE TABLE review_vote (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id   UUID NOT NULL REFERENCES review(id),
    user_id     UUID NOT NULL REFERENCES app_user(id),
    vote_type   VARCHAR(10) NOT NULL CHECK (vote_type IN ('USEFUL','FUNNY','COOL')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (review_id, user_id, vote_type)
);

-- ---------------------------------------------------------------------
-- section: verification (NID)
-- ---------------------------------------------------------------------
CREATE TABLE nid_verification (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id           UUID NOT NULL REFERENCES business(id),
    owner_user_id         UUID NOT NULL,
    encrypted_image_ref   TEXT NOT NULL,   -- pointer/key id into KMS-backed storage, never a raw public URL
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    resolved_by_admin_id  UUID,
    resolved_at           TIMESTAMPTZ,
    image_purge_at        TIMESTAMPTZ,     -- scheduled data-minimization deletion
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_nid_status ON nid_verification(status);
CREATE INDEX ix_nid_business ON nid_verification(business_id);

-- ---------------------------------------------------------------------
-- section: bookmark (Favorites / Collections)
-- ---------------------------------------------------------------------
CREATE TABLE collection (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES app_user(id),
    name        VARCHAR(150),   -- NULL = default/unnamed "My Favorites"
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_collection_user ON collection(user_id);

CREATE TABLE bookmark (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID NOT NULL REFERENCES app_user(id),
    business_id    UUID NOT NULL REFERENCES business(id),
    collection_id  UUID REFERENCES collection(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, business_id)
);
CREATE INDEX ix_bookmark_collection ON bookmark(collection_id);

-- ---------------------------------------------------------------------
-- section: report
-- ---------------------------------------------------------------------
CREATE TABLE report (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_user_id   UUID NOT NULL REFERENCES app_user(id),
    target_type        VARCHAR(20) NOT NULL CHECK (target_type IN ('REVIEW','LISTING')),
    target_id          UUID NOT NULL,
    reason             VARCHAR(20) NOT NULL CHECK (reason IN ('SPAM','FAKE','OFFENSIVE','OTHER')),
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RESOLVED')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_report_reporter_created ON report(reporter_user_id, created_at);
CREATE INDEX ix_report_status ON report(status);

-- ---------------------------------------------------------------------
-- section: moderation (audit trail)
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type         VARCHAR(50) NOT NULL,   -- REPORT / NID_VERIFICATION / REVIEW / BUSINESS_CLAIM ...
    entity_id           UUID NOT NULL,
    action              VARCHAR(50) NOT NULL,   -- APPROVED / REJECTED / HIDDEN / RESTORED ...
    performed_by_admin  UUID NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity ON audit_log(entity_type, entity_id);

-- ---------------------------------------------------------------------
-- section: gallery
-- ---------------------------------------------------------------------
CREATE TABLE business_photo (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id   UUID NOT NULL REFERENCES business(id),
    url           TEXT NOT NULL,
    sort_order    INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_business_photo_business ON business_photo(business_id, sort_order);

-- ---------------------------------------------------------------------
-- section: fakereview (signal breakdown feeding review.suspicion_score)
-- ---------------------------------------------------------------------
CREATE TABLE fake_review_signal (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id     UUID NOT NULL REFERENCES review(id),
    signal_type   VARCHAR(30) NOT NULL
                      CHECK (signal_type IN ('CONTENT_PATTERN','TIMING_PATTERN','RATING_CLUSTERING')),
    score         SMALLINT NOT NULL,
    detail        TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_fake_signal_review ON fake_review_signal(review_id);

-- ---------------------------------------------------------------------
-- section: summary (AI review summary, cached on a per-business row)
-- ---------------------------------------------------------------------
CREATE TABLE business_review_summary (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id                 UUID NOT NULL UNIQUE REFERENCES business(id),
    summary_text                TEXT NOT NULL,
    review_count_at_generation  INTEGER NOT NULL,
    generated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                     BIGINT NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
-- section: messaging (consumer <-> owner direct inquiry thread)
-- ---------------------------------------------------------------------
CREATE TABLE message_thread (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    consumer_user_id   UUID NOT NULL REFERENCES app_user(id),
    business_id        UUID NOT NULL REFERENCES business(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (consumer_user_id, business_id)
);

CREATE TABLE message (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id     UUID NOT NULL REFERENCES message_thread(id),
    sender_user_id UUID NOT NULL REFERENCES app_user(id),
    content       TEXT NOT NULL,
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_message_thread_created ON message(thread_id, created_at);
