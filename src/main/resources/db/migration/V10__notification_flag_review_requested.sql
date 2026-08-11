-- ---------------------------------------------------------------------
-- Extend notification.type: an owner whose listing is flagged now has a
-- next step — "Request review" — which notifies every ADMIN user, rather
-- than leaving the flag as a dead end only an admin happens to notice.
-- Same widen-the-CHECK-constraint pattern as V4/V8.
-- ---------------------------------------------------------------------
ALTER TABLE notification DROP CONSTRAINT notification_type_check;

ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('REPORT_SUBMITTED', 'REPORT_ACTION_TAKEN', 'REPORT_DISMISSED', 'CONTENT_HIDDEN',
                     'LISTING_FLAGGED', 'FLAG_REVIEW_REQUESTED'));
