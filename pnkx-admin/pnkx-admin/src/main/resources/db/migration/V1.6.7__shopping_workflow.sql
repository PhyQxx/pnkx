ALTER TABLE px_meal_plan ADD COLUMN servings DECIMAL(12,3) NOT NULL DEFAULT 1;
UPDATE px_meal_plan m JOIN px_recipe r ON r.id=m.recipe_id SET m.servings=CASE WHEN r.servings>0 THEN r.servings ELSE 1 END;
ALTER TABLE px_shopping_item ADD COLUMN fulfillment_state VARCHAR(24) NOT NULL DEFAULT 'needed', ADD COLUMN revision BIGINT NOT NULL DEFAULT 0, ADD COLUMN claimed_by VARCHAR(64) NULL, ADD COLUMN claimed_at DATETIME NULL, ADD COLUMN batch_id BIGINT NULL;
UPDATE px_shopping_item SET fulfillment_state='purchased' WHERE checked=1;
CREATE TABLE px_operation_preview (
 id VARCHAR(64) PRIMARY KEY, owner_id VARCHAR(64) NOT NULL, kind VARCHAR(32) NOT NULL,
 payload LONGTEXT NOT NULL, source_hash VARCHAR(64) NOT NULL,
 expires_at DATETIME NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'preview',
 result_json LONGTEXT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_preview_owner(owner_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE px_shopping_source (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, list_id BIGINT NOT NULL, item_id BIGINT NOT NULL,
 meal_plan_id BIGINT NOT NULL, ingredient_id BIGINT NOT NULL, plan_date DATE NOT NULL,
 group_key VARCHAR(64) NOT NULL, quantity DECIMAL(12,3) NULL, unit VARCHAR(24) NULL, raw_quantity VARCHAR(255) NULL,
 UNIQUE KEY uk_shopping_source(list_id,meal_plan_id,ingredient_id), KEY ix_shopping_source_list(list_id,group_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE px_shopping_batch (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,list_id BIGINT NOT NULL,owner_id VARCHAR(64) NOT NULL,
 request_id VARCHAR(64) NOT NULL,request_hash VARCHAR(64) NOT NULL,record_id BIGINT NULL,total_amount DECIMAL(18,2) NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_shopping_batch_request(owner_id,request_id), UNIQUE KEY uk_shopping_batch_record(record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
