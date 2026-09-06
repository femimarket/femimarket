SELECT id, client_id, from_date, `to_date`, note, user_id, created_at, updated_note, updated_at
FROM care.critical_visit_rules
WHERE from_date <= :to_date AND `to_date` >= :from_date;
