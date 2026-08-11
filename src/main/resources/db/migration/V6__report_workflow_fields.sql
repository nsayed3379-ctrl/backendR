-- ---------------------------------------------------------------------
-- Report workflow (production-grade): reference codes, admin resolution
-- notes, notification timestamps, auto-triage priority, and a 48h SLA
-- due-date. Backing sequence for reference codes starts at 1000 so codes
-- read as "R-1000", "R-1001", ... rather than the visually-thin "R-0001".
-- ---------------------------------------------------------------------
CREATE SEQUENCE report_reference_seq START WITH 1000;

ALTER TABLE report ADD COLUMN reference_code VARCHAR(20);
ALTER TABLE report ADD COLUMN resolution_note TEXT;
ALTER TABLE report ADD COLUMN reporter_notified_at TIMESTAMPTZ;
ALTER TABLE report ADD COLUMN target_owner_notified_at TIMESTAMPTZ;
ALTER TABLE report ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('HIGH', 'NORMAL'));
ALTER TABLE report ADD COLUMN due_at TIMESTAMPTZ;

-- Backfill existing rows: a fresh reference code per row (nextval() is
-- volatile, so this assigns a distinct sequence value per row) and a due_at
-- derived from their original created_at + 48h, matching the new rule.
UPDATE report SET reference_code = 'R-' || nextval('report_reference_seq')::text WHERE reference_code IS NULL;
UPDATE report SET due_at = created_at + INTERVAL '48 hours' WHERE due_at IS NULL;

ALTER TABLE report ALTER COLUMN reference_code SET NOT NULL;
ALTER TABLE report ALTER COLUMN due_at SET NOT NULL;
ALTER TABLE report ADD CONSTRAINT ux_report_reference_code UNIQUE (reference_code);

CREATE INDEX ix_report_due_at ON report(due_at);
CREATE INDEX ix_report_priority ON report(priority);
