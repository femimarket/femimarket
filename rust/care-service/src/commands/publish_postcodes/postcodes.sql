SELECT id
FROM care.postcodes
WHERE id IN (SELECT candidate FROM JSON_TABLE(:candidates, '$[*]' COLUMNS (candidate VARCHAR(255) PATH '$')) AS candidates);
