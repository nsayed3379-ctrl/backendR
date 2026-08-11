-- ---------------------------------------------------------------------
-- Extend notification.type: business-listing owners now get notified too
-- (LISTING_FLAGGED), alongside the existing review-author CONTENT_HIDDEN
-- notification, when a report against their target is resolved ACTION_TAKEN.
-- Same widen-the-CHECK-constraint pattern as V4__allow_admin_role.sql.
-- ---------------------------------------------------------------------
ALTER TABLE notification DROP CONSTRAINT notification_type_check;

ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('REPORT_SUBMITTED', 'REPORT_ACTION_TAKEN', 'REPORT_DISMISSED', 'CONTENT_HIDDEN', 'LISTING_FLAGGED'));
