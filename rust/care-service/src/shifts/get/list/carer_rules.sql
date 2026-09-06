SELECT id, shift_id, requires_gender, note, user_id, created_at, cancelled_note, cancelled_at
FROM care.carer_rules
WHERE cancelled_at IS NULL;
