-- ---------------------------------------------------------------------
-- Report workflow (production-grade): replace the binary PENDING/RESOLVED
-- status with an outcome-carrying enum. "RESOLVED" never distinguished why
-- a report was closed, so there's no way to retroactively tell which old
-- rows were genuine violations vs. dismissed vs. duplicates — every
-- pre-existing RESOLVED row is mapped to ACTION_TAKEN, since the old flow
-- only ever marked something "resolved" after an admin had already acted on
-- it (there was no dismiss-without-action path in the old UI).
-- ---------------------------------------------------------------------
ALTER TABLE report DROP CONSTRAINT report_status_check;

UPDATE report SET status = 'ACTION_TAKEN' WHERE status = 'RESOLVED';

ALTER TABLE report ADD CONSTRAINT report_status_check
    CHECK (status IN ('PENDING', 'ACTION_TAKEN', 'DISMISSED', 'DUPLICATE'));
