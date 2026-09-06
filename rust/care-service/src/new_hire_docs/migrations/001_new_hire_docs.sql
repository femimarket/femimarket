CREATE TABLE IF NOT EXISTS care.new_hire_docs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    candidate_id VARCHAR(255) NOT NULL,
    kind VARCHAR(255) NOT NULL,
    file VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (candidate_id) REFERENCES care.users (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
