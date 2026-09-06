CREATE TABLE IF NOT EXISTS care.time_rules (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    earliest_start TIME NOT NULL,
    latest_start TIME NOT NULL,
    ideal_start TIME NULL,
    permissioned_from TIME NULL,
    permissioned_to TIME NULL,
    duration_mins INT NOT NULL,
    max_late_mins INT NOT NULL,
    call_type ENUM('breakfast30', 'breakfast45', 'breakfast60', 'lunch30', 'lunch45', 'lunch60', 'tea30', 'tea45', 'tea60', 'bedtime30', 'bedtime45', 'bedtime60', 'long_hours', 'waking_night', 'live_in') NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (client_id) REFERENCES care.clients (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
