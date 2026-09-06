UPDATE care.users
SET user_type = :user_type, first_name = :first_name, last_name = :last_name
WHERE id = :id;
