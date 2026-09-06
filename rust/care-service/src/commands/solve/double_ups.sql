SELECT id, staff_id, from_date, `to_date`, note, user_id, created_at
FROM care.double_ups
WHERE from_date <= :to_date AND `to_date` >= :from_date;
