-- ---------------------------------------------------------------------
-- Notification system: generic per-user notification rows, one row per
-- (event, channel) pair — an event that fans out to both IN_APP and SMS
-- (e.g. a review being hidden) produces two rows with the same content.
-- The `type` values below cover the report workflow; kept extensible via
-- future migrations the same way every other enum column in this schema is
-- (see V4__allow_admin_role.sql for the precedent).
-- ---------------------------------------------------------------------
CREATE TABLE notification (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_user_id     UUID NOT NULL REFERENCES app_user(id),
    type                  VARCHAR(30) NOT NULL
        CHECK (type IN ('REPORT_SUBMITTED', 'REPORT_ACTION_TAKEN', 'REPORT_DISMISSED', 'CONTENT_HIDDEN')),
    title                 VARCHAR(200) NOT NULL,
    body                  TEXT NOT NULL,
    related_entity_type   VARCHAR(30),
    related_entity_id     UUID,
    channel               VARCHAR(10) NOT NULL CHECK (channel IN ('SMS', 'IN_APP')),
    status                VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at               TIMESTAMPTZ
);

-- Backs both the inbox list (recipient + channel, newest first) and the
-- unread-count query (recipient + channel + status <> READ).
CREATE INDEX ix_notification_recipient_channel_created ON notification(recipient_user_id, channel, created_at);
CREATE INDEX ix_notification_recipient_channel_status ON notification(recipient_user_id, channel, status);
