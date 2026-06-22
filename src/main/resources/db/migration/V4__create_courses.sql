CREATE TABLE courses (
    id                BIGSERIAL      PRIMARY KEY,
    title             VARCHAR(255)   NOT NULL,
    description       TEXT,
    duration_in_weeks INT            NOT NULL CHECK (duration_in_weeks > 0),
    price             NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    status            VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_courses_status ON courses (status);
