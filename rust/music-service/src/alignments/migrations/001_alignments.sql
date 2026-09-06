CREATE TABLE IF NOT EXISTS music.alignments (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    audio_id INT NOT NULL,
    text VARCHAR(255) NOT NULL,
    start DOUBLE NOT NULL,
    `end` DOUBLE NOT NULL,
    FOREIGN KEY (audio_id) REFERENCES music.assets (id)
);
