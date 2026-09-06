SELECT shifts.id, shifts.client_id, clients.postcode_id
FROM care.shifts
JOIN care.clients ON clients.id = shifts.client_id
WHERE shifts.cancelled_at IS NULL AND shifts.on_date = :on_date;
