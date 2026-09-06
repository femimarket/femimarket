CREATE TABLE IF NOT EXISTS music.faqs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    audio_id INT NOT NULL,
    question VARCHAR(255) NOT NULL,
    answer VARCHAR(255) NULL,
    FOREIGN KEY (audio_id) REFERENCES music.assets (id)
);
