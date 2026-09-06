CREATE TABLE IF NOT EXISTS care.banned_double_ups (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    partner_id INT NOT NULL,
    from_date DATE NOT NULL,
    `to_date` DATE NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_note VARCHAR(255) NULL,
    updated_at TIMESTAMP NULL,
    FOREIGN KEY (staff_id) REFERENCES care.staffs (id),
    FOREIGN KEY (partner_id) REFERENCES care.staffs (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
