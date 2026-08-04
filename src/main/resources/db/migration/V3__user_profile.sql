ALTER TABLE app_user ADD COLUMN name VARCHAR(120);
ALTER TABLE app_user ADD COLUMN profile_photo_url TEXT;
ALTER TABLE app_user ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'en';
