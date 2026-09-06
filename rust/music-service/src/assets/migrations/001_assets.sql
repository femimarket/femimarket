CREATE TABLE IF NOT EXISTS music.assets (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    asset_type ENUM('Song', 'Instrumental', 'FrontCover', 'Protagonist', 'Scene', 'Video') NOT NULL,
    FOREIGN KEY (user_id) REFERENCES matrix.users (id)
);
