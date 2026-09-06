ALTER TABLE care.users DROP FOREIGN KEY IF EXISTS users_ibfk_1;
ALTER TABLE care.users DROP FOREIGN KEY IF EXISTS users_ibfk_2;
ALTER TABLE care.users CHANGE COLUMN IF EXISTS owner_id matrix_id INT NULL;
ALTER TABLE care.users MODIFY COLUMN matrix_id INT NULL;
ALTER TABLE care.users ADD CONSTRAINT users_matrix_id FOREIGN KEY IF NOT EXISTS users_matrix_id (matrix_id) REFERENCES matrix.users (id);
ALTER TABLE care.users ADD COLUMN IF NOT EXISTS parent_id VARCHAR(255) NULL;
ALTER TABLE care.users ADD CONSTRAINT users_parent_id FOREIGN KEY IF NOT EXISTS users_parent_id (parent_id) REFERENCES care.users (id);
ALTER TABLE care.users DROP FOREIGN KEY IF EXISTS users_creator_id;
ALTER TABLE care.users DROP COLUMN IF EXISTS creator_id;
