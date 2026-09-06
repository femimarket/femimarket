CREATE TABLE IF NOT EXISTS music.asset_links (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    a_id INT NOT NULL,
    b_id INT NOT NULL,
    FOREIGN KEY (a_id) REFERENCES music.assets (id),
    FOREIGN KEY (b_id) REFERENCES music.assets (id)
);
