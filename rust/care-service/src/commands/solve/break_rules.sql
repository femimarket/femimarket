SELECT id, staff_id, break_mins, break_required_after_mins, from_date, `to_date`, note, user_id, created_at, updated_note, updated_at
FROM care.break_rules
WHERE from_date <= :to_date AND `to_date` >= :from_date;
