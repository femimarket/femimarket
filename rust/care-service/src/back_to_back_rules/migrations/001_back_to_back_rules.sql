CREATE TABLE IF NOT EXISTS care.back_to_back_rules (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift1_id INT NOT NULL,
    shift2_id INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (shift1_id) REFERENCES care.shifts (id),
    FOREIGN KEY (shift2_id) REFERENCES care.shifts (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
