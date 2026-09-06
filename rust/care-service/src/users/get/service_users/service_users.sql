SELECT id, user_type, first_name, last_name, matrix_id, parent_id, created_at
FROM care.users
WHERE user_type = 'ServiceUser';
