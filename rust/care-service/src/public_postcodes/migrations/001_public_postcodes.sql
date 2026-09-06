CREATE TABLE IF NOT EXISTS care.public_postcodes (
    postcode_id VARCHAR(255) NOT NULL PRIMARY KEY,
    public_postcode_id VARCHAR(255) NOT NULL UNIQUE,
    note VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES care.users (id)
);
