ALTER TABLE images
DROP CONSTRAINT IF EXISTS images_file_name_key;

ALTER TABLE images
DROP CONSTRAINT IF EXISTS images_file_path_key;