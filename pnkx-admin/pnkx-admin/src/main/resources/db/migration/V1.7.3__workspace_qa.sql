CREATE TABLE px_workspace_qa (
 request_id VARCHAR(64) NOT NULL, owner_id VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL, answer LONGTEXT, sources_json LONGTEXT,
 coverage VARCHAR(300), input_chars INT NOT NULL DEFAULT 0, output_chars INT NOT NULL DEFAULT 0,
 elapsed_ms BIGINT NOT NULL DEFAULT 0, target_type VARCHAR(20), target_id BIGINT,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(owner_id,request_id), KEY idx_workspace_qa_owner_time(owner_id,create_time)
);
