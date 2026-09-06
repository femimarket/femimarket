CREATE TABLE IF NOT EXISTS care.preferred_workers (
    preference_id INT NOT NULL,
    staff_id INT NOT NULL,
    PRIMARY KEY (preference_id, staff_id),
    FOREIGN KEY (preference_id) REFERENCES care.preferences (id),
    FOREIGN KEY (staff_id) REFERENCES care.staffs (id)
);
