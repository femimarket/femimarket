SELECT id, user_id, name, asset_type
FROM music.assets
WHERE name = :name;
