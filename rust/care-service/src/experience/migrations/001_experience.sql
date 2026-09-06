CREATE TABLE IF NOT EXISTS care.experience (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NULL,
    staff_id INT NULL,
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id),
    FOREIGN KEY (staff_id) REFERENCES care.staffs (id)
);
