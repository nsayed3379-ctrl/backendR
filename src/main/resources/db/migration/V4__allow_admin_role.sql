-- V1__init.sql defined app_user.role with CHECK (role IN ('CONSUMER','BUSINESS_OWNER')).
-- UserRole.ADMIN has existed in the Java enum since the start (used throughout
-- moderation/report/claim/NID services via common.CurrentUser.requireRole("ADMIN")),
-- but the DB check constraint was never updated to allow it — so no ADMIN row
-- could ever be inserted. This adds it without touching the already-applied V1.
ALTER TABLE app_user DROP CONSTRAINT app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check CHECK (role IN ('CONSUMER','BUSINESS_OWNER','ADMIN'));
