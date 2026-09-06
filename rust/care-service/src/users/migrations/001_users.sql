CREATE TABLE IF NOT EXISTS care.users (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    user_type ENUM('ServiceUser', 'Staff', 'Candidate', 'Management') NOT NULL,
    first_name VARCHAR(255) NULL,
    last_name VARCHAR(255) NULL,
    owner_id INT NOT NULL,
    creator_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES matrix.users (id),
    FOREIGN KEY (creator_id) REFERENCES matrix.users (id)
);
