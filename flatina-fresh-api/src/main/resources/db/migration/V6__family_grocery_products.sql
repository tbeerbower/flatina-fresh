-- Saved grocery products (brands, package sizes, staples) belong to a family; existing ones go to the
-- first admin's family. The table is rebuilt to swap its unique key from ingredient_key to (family_id, ingredient_key) portably; nothing refers to product ids.
ALTER TABLE grocery_product RENAME TO grocery_product_old;
CREATE TABLE grocery_product (
    id              BIGSERIAL PRIMARY KEY,
    family_id       BIGINT       NOT NULL REFERENCES family (id) ON DELETE CASCADE,
    ingredient_key  VARCHAR(200) NOT NULL,
    ingredient_name VARCHAR(200) NOT NULL,
    brand           VARCHAR(100),
    product_name    VARCHAR(200),
    package_size    NUMERIC(10, 3),
    package_unit    VARCHAR(50),
    package_label   VARCHAR(50),
    store_item      VARCHAR(500),
    staple          BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_grocery_product_family_key UNIQUE (family_id, ingredient_key)
);
INSERT INTO grocery_product (family_id, ingredient_key, ingredient_name, brand, product_name, package_size,
                             package_unit, package_label, store_item, staple, updated_at)
SELECT (SELECT u.family_id FROM app_user u ORDER BY u.admin DESC, u.id LIMIT 1),
       ingredient_key, ingredient_name, brand, product_name, package_size, package_unit, package_label, store_item,
       staple, updated_at
FROM grocery_product_old
WHERE EXISTS (SELECT 1 FROM app_user);
DROP TABLE grocery_product_old;
