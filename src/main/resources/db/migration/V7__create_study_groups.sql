CREATE TABLE study_groups (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    course_id  BIGINT       NOT NULL REFERENCES courses (id),
    teacher_id BIGINT       NOT NULL REFERENCES users (id),
    start_date DATE         NOT NULL,
    status     VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE study_group_students (
    study_group_id BIGINT NOT NULL REFERENCES study_groups (id) ON DELETE CASCADE,
    student_id     BIGINT NOT NULL REFERENCES students (id)     ON DELETE CASCADE,
    PRIMARY KEY (study_group_id, student_id)
);

CREATE INDEX idx_study_groups_course_id  ON study_groups (course_id);
CREATE INDEX idx_study_groups_teacher_id ON study_groups (teacher_id);
CREATE INDEX idx_study_groups_status     ON study_groups (status);
