CREATE TABLE IF NOT EXISTS care.whitelists (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_note VARCHAR(255) NULL,
    updated_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
