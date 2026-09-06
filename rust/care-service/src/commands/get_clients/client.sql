SELECT id, name, postcode_id, roundsys_pk
FROM care.clients
WHERE roundsys_pk = :roundsys_pk;
