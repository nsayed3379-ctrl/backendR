-- ---------------------------------------------------------------------
-- Business cards show a circular profile/logo picture (business owner's
-- headshot or shop logo) distinct from the cover/gallery photos used as
-- the card's background carousel.
-- ---------------------------------------------------------------------
ALTER TABLE business ADD COLUMN logo_url TEXT;
