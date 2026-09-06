CREATE TABLE IF NOT EXISTS care.assignments (
    rota_id INT NOT NULL,
    shift_id INT NOT NULL,
    staff_id INT NOT NULL,
    assignment_type_id VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (rota_id, shift_id, staff_id),
    FOREIGN KEY (rota_id) REFERENCES care.rota (id),
    FOREIGN KEY (shift_id) REFERENCES care.shifts (id),
    FOREIGN KEY (staff_id) REFERENCES care.staffs (id),
    FOREIGN KEY (assignment_type_id) REFERENCES care.assignment_types (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
