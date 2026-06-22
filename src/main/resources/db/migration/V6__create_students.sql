CREATE TABLE students (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       UNIQUE REFERENCES users (id) ON DELETE SET NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(20),
    email      VARCHAR(255),
    birth_date DATE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Теперь, когда students существует, добавляем FK в leads
ALTER TABLE leads
    ADD CONSTRAINT fk_leads_converted_student
    FOREIGN KEY (converted_student_id) REFERENCES students (id) ON DELETE SET NULL;

CREATE INDEX idx_students_email ON students (email);
