CREATE TABLE IF NOT EXISTS concerts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tickets (
    id SERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    booking_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL
);