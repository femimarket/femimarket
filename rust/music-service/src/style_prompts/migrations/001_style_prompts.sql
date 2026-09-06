CREATE TABLE IF NOT EXISTS music.style_prompts (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    song_id INT NOT NULL,
    text VARCHAR(255) NOT NULL,
    FOREIGN KEY (song_id) REFERENCES music.assets (id)
);
