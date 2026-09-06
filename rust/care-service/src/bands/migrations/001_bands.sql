CREATE TABLE IF NOT EXISTS care.bands (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    from_time TIME NOT NULL,
    to_time TIME NOT NULL,
    flex_before_mins INT NOT NULL,
    flex_after_mins INT NOT NULL,
    max_late_mins INT NOT NULL,
    note VARCHAR(255) NOT NULL
);
