CREATE TABLE IF NOT EXISTS care.activity_links (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    activity_id VARCHAR(255) NOT NULL,
    sub_activity_id VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (activity_id) REFERENCES care.activities (id),
    FOREIGN KEY (sub_activity_id) REFERENCES care.activities (id)
);
