SELECT id, staff_id, passengers, from_date, `to_date`, note, user_id, created_at, updated_note, updated_at
FROM care.passenger_rules
WHERE from_date <= :to_date AND `to_date` >= :from_date;
