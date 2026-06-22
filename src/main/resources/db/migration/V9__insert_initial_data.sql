-- Роли
INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('MANAGER'),
    ('TEACHER'),
    ('STUDENT')
ON CONFLICT (name) DO NOTHING;

-- Системный администратор по умолчанию
-- Логин:  admin@covenantcode.ru
-- Пароль: Admin123!  (BCrypt hash of "password", cost=10)
-- ВАЖНО: сменить пароль через API после первого запуска!
INSERT INTO users (first_name, last_name, email, password, role_id, enabled)
VALUES (
    'Admin',
    'System',
    'admin@covenantcode.ru',
    '$2a$10$DPQBRuyeFJDlHcmOuhVviOJHFpTwQTA5iOqX6l.tVqCDT4CSL8WtC',
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    TRUE
)
ON CONFLICT (email) DO NOTHING;
