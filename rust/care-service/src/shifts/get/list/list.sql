SELECT shifts.id, shifts.client_id, shifts.on_date, shifts.time_rule_id, shifts.whitelist_id,
       shifts.blacklist_id, shifts.preference_id, shifts.double_up_id, shifts.roundsys_pk,
       shifts.note, shifts.user_id, shifts.created_at, shifts.cancelled_note, shifts.cancelled_at,
       time_rules.earliest_start, time_rules.latest_start, time_rules.ideal_start,
       time_rules.permissioned_from, time_rules.permissioned_to, time_rules.duration_mins,
       time_rules.max_late_mins, time_rules.call_type,
       clients.name AS client_name, clients.postcode_id AS client_postcode_id
FROM care.shifts
JOIN care.time_rules ON time_rules.id = shifts.time_rule_id
JOIN care.clients ON clients.id = shifts.client_id
WHERE shifts.cancelled_at IS NULL AND shifts.on_date >= :on_date;
