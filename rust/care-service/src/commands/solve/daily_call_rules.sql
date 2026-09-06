SELECT id, client_id, max_carers, max_calls_per_carer, from_date, `to_date`, note, user_id, created_at
FROM care.daily_call_rules
WHERE from_date <= :to_date AND `to_date` >= :from_date;
