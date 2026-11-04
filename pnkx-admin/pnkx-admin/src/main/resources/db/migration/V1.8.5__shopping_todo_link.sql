CREATE TABLE IF NOT EXISTS px_shopping_todo_link (
    item_id BIGINT NOT NULL PRIMARY KEY,
    todo_id BIGINT NOT NULL,
    KEY idx_shopping_todo (todo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购待办与购物项关联';
