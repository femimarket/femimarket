SELECT rota_id, shift_id, staff_id, assignment_type_id, start_time, end_time, note, user_id, created_at
FROM care.assignments
WHERE rota_id = :rota_id;
