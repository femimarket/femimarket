SELECT id, staff_id, from_date, `to_date`, start_time, end_time, note, user_id, created_at, updated_note, updated_at, approved_note, approved
FROM care.availabilities
WHERE updated_at IS NULL AND from_date <= :to_date AND `to_date` >= :from_date;
