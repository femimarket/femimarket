CREATE TABLE IF NOT EXISTS care.weight_penalty_partial_double_up (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    penalty INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
