-- Business-claim verification overhaul: PHONE and EMAIL now verify instantly
-- (OTP/code challenge, no admin in the loop); DOCUMENT replaces the removed
-- NID method as the manual-review fallback (e.g. utility bill upload).

ALTER TABLE business_claim DROP CONSTRAINT IF EXISTS business_claim_verification_method_check;

-- Pre-existing rows filed under the removed NID method become DOCUMENT —
-- closest equivalent (an uploaded proof reviewed by an admin).
UPDATE business_claim SET verification_method = 'DOCUMENT' WHERE verification_method = 'NID';

ALTER TABLE business_claim ADD CONSTRAINT business_claim_verification_method_check
    CHECK (verification_method IN ('PHONE','EMAIL','DOCUMENT'));

ALTER TABLE business_claim ADD COLUMN document_ref TEXT;

-- Email-channel counterpart to otp_verification (same shape, keyed by email).
CREATE TABLE email_verification (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    attempts        INTEGER NOT NULL DEFAULT 0,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_email_verification_email_created ON email_verification(email, created_at);
