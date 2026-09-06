SELECT id, staff_id, partner_id, from_date, `to_date`, note, user_id, created_at, updated_note, updated_at
FROM care.banned_double_ups
WHERE from_date <= :to_date AND `to_date` >= :from_date;
