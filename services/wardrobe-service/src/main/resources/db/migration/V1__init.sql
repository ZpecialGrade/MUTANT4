CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS profiles (
	id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id uuid NOT NULL UNIQUE,
	display_name varchar(80) NOT NULL,
	created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS items (
	id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	profile_id uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
	name varchar(120) NOT NULL,
	color varchar(40) NOT NULL,
	type varchar(32) NOT NULL,
	image_object_key varchar(512) NOT NULL,
	created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_items_profile_id ON items(profile_id);
CREATE INDEX IF NOT EXISTS idx_items_type ON items(type);
CREATE INDEX IF NOT EXISTS idx_items_color ON items(color);

CREATE TABLE IF NOT EXISTS user_photos (
	id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	profile_id uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
	image_object_key varchar(512) NOT NULL,
	created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_photos_profile_id ON user_photos(profile_id);

CREATE TABLE IF NOT EXISTS looks (
	id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	profile_id uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
	name varchar(120) NULL,
	result_image_object_key varchar(512) NOT NULL,
	source_user_photo_id uuid NOT NULL REFERENCES user_photos(id) ON DELETE RESTRICT,
	created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_looks_profile_id ON looks(profile_id);
CREATE INDEX IF NOT EXISTS idx_looks_created_at ON looks(created_at);

CREATE TABLE IF NOT EXISTS look_items (
	look_id uuid NOT NULL REFERENCES looks(id) ON DELETE CASCADE,
	item_id uuid NOT NULL REFERENCES items(id) ON DELETE CASCADE,
	PRIMARY KEY (look_id, item_id)
);

