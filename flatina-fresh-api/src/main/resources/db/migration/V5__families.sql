-- Families share one meal plan. Every user belongs to exactly one family; members invite others with a link.
-- Runs after V4 (grocery products), which it scopes to families at the end.
CREATE TABLE family (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    created_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Each existing user gets a family of their own.
INSERT INTO family (name, created_by)
SELECT COALESCE(display_name, username) || '''s family', id FROM app_user ORDER BY id;

ALTER TABLE app_user ADD COLUMN family_id BIGINT REFERENCES family (id);
UPDATE app_user SET family_id = (SELECT f.id FROM family f WHERE f.created_by = app_user.id);
ALTER TABLE app_user ALTER COLUMN family_id SET NOT NULL;
CREATE INDEX idx_app_user_family ON app_user (family_id);

-- The plan so far was single-user: it moves to the family of the first admin (or of the first user).
ALTER TABLE meal_plan_entry ADD COLUMN family_id BIGINT REFERENCES family (id) ON DELETE CASCADE;
UPDATE meal_plan_entry SET family_id = (
    SELECT u.family_id FROM app_user u ORDER BY u.admin DESC, u.id LIMIT 1);
DELETE FROM meal_plan_entry WHERE family_id IS NULL;
ALTER TABLE meal_plan_entry ALTER COLUMN family_id SET NOT NULL;
CREATE INDEX idx_meal_plan_entry_family_date ON meal_plan_entry (family_id, plan_date);

-- A single-use link that creates an account in the family.
CREATE TABLE family_invite (
    id         BIGSERIAL PRIMARY KEY,
    family_id  BIGINT      NOT NULL REFERENCES family (id) ON DELETE CASCADE,
    token      VARCHAR(64) NOT NULL UNIQUE,
    created_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    used_by    BIGINT REFERENCES app_user (id) ON DELETE SET NULL
);
CREATE INDEX idx_family_invite_family ON family_invite (family_id);

-- Saved grocery products (brands, package sizes, staples) belong to a family too. The table is rebuilt to swap its
-- unique key from ingredient_key to (family_id, ingredient_key) portably; nothing refers to product ids.
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
