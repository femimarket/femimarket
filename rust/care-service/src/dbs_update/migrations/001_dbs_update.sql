CREATE TABLE IF NOT EXISTS care.dbs_update (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dbs_id INT NOT NULL,
    status VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (dbs_id) REFERENCES care.dbs (id),
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
