CREATE TABLE IF NOT EXISTS care.skills (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    care_id VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL,
    FOREIGN KEY (care_id) REFERENCES care.cares (id)
);
