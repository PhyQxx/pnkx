CREATE TABLE px_note_sync_cursor (
 owner_id VARCHAR(64) NOT NULL PRIMARY KEY,
 last_seq BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE px_note_change (
 owner_id VARCHAR(64) NOT NULL,
 seq BIGINT NOT NULL,
 note_id BIGINT NOT NULL,
 PRIMARY KEY(owner_id,seq),
 KEY idx_note_change_resource(owner_id,note_id)
);
INSERT INTO px_note_sync_cursor(owner_id,last_seq) SELECT create_by,count(*) FROM px_note WHERE create_by IS NOT NULL GROUP BY create_by;
INSERT INTO px_note_change(owner_id,seq,note_id) SELECT create_by,ROW_NUMBER() OVER(PARTITION BY create_by ORDER BY id),id FROM px_note WHERE create_by IS NOT NULL;
