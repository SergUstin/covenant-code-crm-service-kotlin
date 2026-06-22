CREATE TABLE leads (
    id                   BIGSERIAL    PRIMARY KEY,
    first_name           VARCHAR(100) NOT NULL,
    last_name            VARCHAR(100),
    phone                VARCHAR(20)  NOT NULL,
    email                VARCHAR(255),
    source               VARCHAR(255),
    interested_course_id BIGINT       REFERENCES courses (id) ON DELETE SET NULL,
    status               VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    comment              TEXT,
    assigned_manager_id  BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    -- converted_student_id без FK: таблица students ещё не создана.
    -- FK будет добавлен в V6 через ALTER TABLE.
    converted_student_id BIGINT       UNIQUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE lead_comments (
    id         BIGSERIAL   PRIMARY KEY,
    lead_id    BIGINT      NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id),
    text       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_leads_status           ON leads (status);
CREATE INDEX idx_leads_assigned_manager ON leads (assigned_manager_id);
CREATE INDEX idx_lead_comments_lead_id  ON lead_comments (lead_id);
