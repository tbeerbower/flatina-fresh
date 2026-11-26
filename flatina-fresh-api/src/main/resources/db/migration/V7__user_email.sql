-- People sign in with their email address. Emails are stored lowercased, so a plain UNIQUE is case-insensitive.
-- Existing accounts have no email yet (NULL); they can still sign in with their username until they add one.
ALTER TABLE app_user ADD COLUMN email VARCHAR(254);
ALTER TABLE app_user ADD CONSTRAINT uq_app_user_email UNIQUE (email);

-- The username is now just the name the app calls someone, so it no longer has to be unique. The column is rebuilt
-- because the old unique constraint has a database-generated name (dropping the column drops it portably).
ALTER TABLE app_user ADD COLUMN username_new VARCHAR(100);
UPDATE app_user SET username_new = username;
ALTER TABLE app_user DROP COLUMN username;
ALTER TABLE app_user RENAME COLUMN username_new TO username;
ALTER TABLE app_user ALTER COLUMN username SET NOT NULL;
