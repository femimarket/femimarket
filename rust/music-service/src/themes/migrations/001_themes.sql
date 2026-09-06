CREATE TABLE IF NOT EXISTS music.themes (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    line_id INT NOT NULL,
    theme VARCHAR(255) NOT NULL,
    expand VARCHAR(255) NULL,
    scene VARCHAR(255) NULL,
    FOREIGN KEY (line_id) REFERENCES music.lines (id)
);
