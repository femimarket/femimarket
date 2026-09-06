CREATE TABLE IF NOT EXISTS care.gender_rules (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    requires_gender VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
