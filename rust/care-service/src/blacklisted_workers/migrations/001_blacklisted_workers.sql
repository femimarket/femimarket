CREATE TABLE IF NOT EXISTS care.blacklisted_workers (
    blacklist_id INT NOT NULL,
    staff_id INT NOT NULL,
    PRIMARY KEY (blacklist_id, staff_id),
    FOREIGN KEY (blacklist_id) REFERENCES care.blacklists (id),
    FOREIGN KEY (staff_id) REFERENCES care.staffs (id)
);
