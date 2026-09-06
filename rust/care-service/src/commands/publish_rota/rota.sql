SELECT id, from_date, `to_date`, wall_seconds, note, user_id, created_at
FROM care.rota
WHERE id = :id;
