CREATE TABLE IF NOT EXISTS care.weight_solver_workers (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workers INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
