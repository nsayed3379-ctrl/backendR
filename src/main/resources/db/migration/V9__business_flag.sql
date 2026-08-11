-- ---------------------------------------------------------------------
-- Report workflow (production-grade), part 2: resolving a LISTING report as
-- ACTION_TAKEN must actually do something to the listing, not just notify —
-- flags it with the report's reason, shown publicly (a "consumer alert",
-- Yelp-style) and on the owner's dashboard. This is a visible warning, not a
-- takedown; admins can still soft-delete a listing manually from the
-- Businesses admin screen if the situation warrants going further.
-- ---------------------------------------------------------------------
ALTER TABLE business ADD COLUMN flagged BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE business ADD COLUMN flag_reason VARCHAR(20);
ALTER TABLE business ADD COLUMN flagged_at TIMESTAMPTZ;

-- Partial index: only a small minority of listings will ever be flagged.
CREATE INDEX ix_business_flagged ON business(flagged) WHERE flagged = true;
