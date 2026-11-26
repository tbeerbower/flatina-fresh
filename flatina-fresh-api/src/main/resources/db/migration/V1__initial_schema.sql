CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(200),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recipe (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(300) NOT NULL,
    subtitle      VARCHAR(500),
    description   TEXT,
    source        VARCHAR(100),
    prep_minutes  INTEGER,
    total_minutes INTEGER,
    calories      INTEGER,
    tags          VARCHAR(500),
    equipment     VARCHAR(1000),
    notes         TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ingredients shipped in the box (scaled per serving size)
CREATE TABLE recipe_ingredient (
    id          BIGSERIAL PRIMARY KEY,
    recipe_id   BIGINT       NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position    INTEGER      NOT NULL,
    name        VARCHAR(200) NOT NULL,
    quantity2   NUMERIC(10, 3),
    quantity4   NUMERIC(10, 3),
    unit        VARCHAR(50),
    allergens   VARCHAR(200),
    pantry      BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_recipe_ingredient_recipe ON recipe_ingredient (recipe_id);

CREATE TABLE recipe_step (
    id          BIGSERIAL PRIMARY KEY,
    recipe_id   BIGINT       NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position    INTEGER      NOT NULL,
    title       VARCHAR(200),
    instructions TEXT        NOT NULL
);
CREATE INDEX idx_recipe_step_recipe ON recipe_step (recipe_id);

-- Scanned card images (front/back), downsized JPEG
CREATE TABLE recipe_image (
    id           BIGSERIAL PRIMARY KEY,
    recipe_id    BIGINT       NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position     INTEGER      NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_recipe_image_recipe ON recipe_image (recipe_id);

-- A recipe scheduled on a day of the meal plan
CREATE TABLE meal_plan_entry (
    id          BIGSERIAL PRIMARY KEY,
    plan_date   DATE    NOT NULL,
    recipe_id   BIGINT  NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    servings    INTEGER NOT NULL DEFAULT 2,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_meal_plan_entry_date ON meal_plan_entry (plan_date);
