CREATE TABLE cards (
    id           uuid        PRIMARY KEY,
    owner_id     uuid        NOT NULL,
    word         text        NOT NULL,
    translation  text        NOT NULL,
    association  text        NOT NULL,
    image_url    text        NULL,
    visibility   text        NOT NULL,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL
);

CREATE TABLE reviews (
    card_id      uuid        PRIMARY KEY REFERENCES cards(id),
    owner_id     uuid        NOT NULL,
    box          int         NOT NULL,
    due_at       timestamptz NOT NULL
);

CREATE INDEX idx_reviews_owner_id_due_at ON reviews (owner_id, due_at);
