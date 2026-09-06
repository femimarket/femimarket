SELECT id, name, transport_mode_id, gender, postcode_id, roundsys_pk
FROM care.staffs
WHERE roundsys_pk = :roundsys_pk;
