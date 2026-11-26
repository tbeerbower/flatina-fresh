-- The product you buy for an ingredient: preferred brand, package size (used to count packages from recipe
-- amounts), the store's item number or link, and whether it's a staple you always keep at home.
-- One row per ingredient, matched by the shopping list's normalized ingredient name.
CREATE TABLE grocery_product (
    id              BIGSERIAL PRIMARY KEY,
    ingredient_key  VARCHAR(200) NOT NULL UNIQUE,
    ingredient_name VARCHAR(200) NOT NULL,
    brand           VARCHAR(100),
    product_name    VARCHAR(200),
    package_size    NUMERIC(10, 3),
    package_unit    VARCHAR(50),
    package_label   VARCHAR(50),
    store_item      VARCHAR(500),
    staple          BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
