CREATE TABLE IF NOT EXISTS care.experience_cares (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    experience_id INT NOT NULL,
    care_id VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (experience_id) REFERENCES care.experience (id),
    FOREIGN KEY (care_id) REFERENCES care.cares (id)
);
