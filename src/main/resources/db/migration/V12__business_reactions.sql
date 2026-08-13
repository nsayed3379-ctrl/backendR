<<<<<<< HEAD
-- Business-level reactions (Like/Dislike/Love/Wow) on business cards — distinct
-- from review_vote (useful/funny/cool on an individual review). Mirrors
-- review_vote's shape: one row per (business, user, reaction_type), toggled.

ALTER TABLE business
    ADD COLUMN total_like_count    INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_dislike_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_love_count    INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_wow_count     INTEGER NOT NULL DEFAULT 0;

CREATE TABLE business_reaction (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id   UUID NOT NULL REFERENCES business(id),
    user_id       UUID NOT NULL REFERENCES app_user(id),
    reaction_type VARCHAR(10) NOT NULL CHECK (reaction_type IN ('LIKE','DISLIKE','LOVE','WOW')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (business_id, user_id, reaction_type)
);

CREATE INDEX idx_business_reaction_business_id ON business_reaction(business_id);
=======
-- ---------------------------------------------------------------------
-- Business-level reactions: a user reacting directly to a business as a
-- whole (useful/funny/cool), shown as a live count on business cards.
-- Distinct from review_vote, which reacts to one specific review's
-- content — a business card aggregates many reviews with no single review
-- to attach a vote to, so this is its own first-class concept.
-- ---------------------------------------------------------------------
CREATE TABLE business_reaction (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_id    UUID NOT NULL REFERENCES business(id),
    user_id        UUID NOT NULL REFERENCES app_user(id),
    reaction_type  VARCHAR(10) NOT NULL CHECK (reaction_type IN ('USEFUL', 'FUNNY', 'COOL')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (business_id, user_id, reaction_type)
);

-- Backs the batched per-business-page GROUP BY count query on the search/list endpoints.
CREATE INDEX ix_business_reaction_business ON business_reaction(business_id, reaction_type);
>>>>>>> c75f009dc3431256bd307fac47a70595495e4001
