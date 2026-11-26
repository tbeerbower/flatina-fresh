-- Families share one meal plan. Every user belongs to exactly one family; members invite others with a link.
-- (V5, not V4: V4 is taken by the shopping list's grocery products, and flyway out-of-order is on.)
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
