CREATE TABLE IF NOT EXISTS music.lines (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    composition_id INT NOT NULL,
    sort VARCHAR(255) NOT NULL,
    text VARCHAR(255) NOT NULL,
    start_ms DOUBLE NOT NULL,
    context VARCHAR(255) NULL,
    goal VARCHAR(255) NULL,
    FOREIGN KEY (composition_id) REFERENCES music.compositions (id)
);
