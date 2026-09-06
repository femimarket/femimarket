CREATE TABLE IF NOT EXISTS care.carer_rules (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id INT NOT NULL,
    requires_gender VARCHAR(255) NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    cancelled_note VARCHAR(255) NULL,
    cancelled_at TIMESTAMP NULL,
    FOREIGN KEY (shift_id) REFERENCES care.shifts (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
