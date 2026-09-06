CREATE TABLE IF NOT EXISTS care.supervisors (
    supervision_id INT NOT NULL,
    supervisor_staff_id INT NOT NULL,
    PRIMARY KEY (supervision_id, supervisor_staff_id),
    FOREIGN KEY (supervision_id) REFERENCES care.supervisions (id),
    FOREIGN KEY (supervisor_staff_id) REFERENCES care.staffs (id)
);
