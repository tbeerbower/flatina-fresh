-- Main protein of a recipe (beef, pork, chicken, turkey, seafood, vegetarian, ...), used to filter recipes.
-- Existing recipes are classified from their tags and ingredients by ProteinBackfill at startup.
ALTER TABLE recipe ADD COLUMN protein VARCHAR(30);
