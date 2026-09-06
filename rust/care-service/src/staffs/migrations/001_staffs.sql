CREATE TABLE IF NOT EXISTS care.staffs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    transport_mode_id VARCHAR(255) NULL,
    gender VARCHAR(255) NULL,
    postcode_id VARCHAR(255) NOT NULL,
    roundsys_pk VARCHAR(255) NULL UNIQUE,
    FOREIGN KEY (transport_mode_id) REFERENCES care.transport_modes (id)
);
