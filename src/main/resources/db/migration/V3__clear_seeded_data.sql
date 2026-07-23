-- Seeded users are gone with the static-token authenticator; every existing
-- cards.owner_id / reviews.owner_id references a seeded UUID that no real mneme-auth
-- account will ever have. Discarded, not re-pointed (decided by the human).
DELETE FROM reviews;
DELETE FROM cards;
