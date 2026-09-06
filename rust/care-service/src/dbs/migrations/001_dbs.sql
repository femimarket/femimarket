CREATE TABLE IF NOT EXISTS care.dbs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    certificate_number VARCHAR(255) NULL,
    update_service BOOLEAN NOT NULL,
    ref VARCHAR(255) NULL,
    ref_time TIMESTAMP NULL,
    paid_note VARCHAR(255) NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
