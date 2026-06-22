CREATE TABLE lessons (
    id             BIGSERIAL    PRIMARY KEY,
    study_group_id BIGINT       NOT NULL REFERENCES study_groups (id) ON DELETE CASCADE,
    teacher_id     BIGINT       NOT NULL REFERENCES users (id),
    topic          VARCHAR(500) NOT NULL,
    description    TEXT,
    lesson_date    DATE         NOT NULL,
    start_time     TIME         NOT NULL,
    end_time       TIME         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_lesson_times CHECK (end_time > start_time)
);

CREATE INDEX idx_lessons_study_group_id ON lessons (study_group_id);
CREATE INDEX idx_lessons_teacher_id     ON lessons (teacher_id);
CREATE INDEX idx_lessons_date           ON lessons (lesson_date);
