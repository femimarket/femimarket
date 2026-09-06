CREATE TABLE IF NOT EXISTS care.daily_call_rules (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    max_carers INT NOT NULL,
    max_calls_per_carer INT NOT NULL,
    from_date DATE NOT NULL,
    `to_date` DATE NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (client_id) REFERENCES care.clients (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
