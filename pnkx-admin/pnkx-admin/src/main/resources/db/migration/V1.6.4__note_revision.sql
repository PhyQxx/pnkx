-- Independent from the legacy version string. Existing notes start at revision 1.
ALTER TABLE px_note ADD COLUMN revision BIGINT NOT NULL DEFAULT 1 COMMENT 'Optimistic concurrency revision';
