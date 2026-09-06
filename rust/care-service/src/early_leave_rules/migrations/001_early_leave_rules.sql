CREATE TABLE IF NOT EXISTS care.early_leave_rules (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id INT NOT NULL,
    max_early_leave_mins INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (shift_id) REFERENCES care.shifts (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
