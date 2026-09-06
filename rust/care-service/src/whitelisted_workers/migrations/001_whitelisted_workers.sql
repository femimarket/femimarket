CREATE TABLE IF NOT EXISTS care.whitelisted_workers (
    whitelist_id INT NOT NULL,
    staff_id INT NOT NULL,
    PRIMARY KEY (whitelist_id, staff_id),
    FOREIGN KEY (whitelist_id) REFERENCES care.whitelists (id),
    FOREIGN KEY (staff_id) REFERENCES care.staffs (id)
);
