CREATE TABLE IF NOT EXISTS care.weight_min_same_client_gap_mins (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    mins INT NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
