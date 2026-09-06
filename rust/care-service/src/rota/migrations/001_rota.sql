CREATE TABLE IF NOT EXISTS care.rota (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    from_date DATE NOT NULL,
    `to_date` DATE NOT NULL,
    wall_seconds DOUBLE NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
