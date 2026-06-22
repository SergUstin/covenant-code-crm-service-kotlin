# CRM Service

REST API бэкенд CRM-системы для образовательного центра. Управляет лидами, студентами, курсами, учебными группами и расписанием занятий с ролевым доступом через JWT.

## Стек

| Слой | Технология |
|------|-----------|
| Framework | Spring Boot 3.3.5 |
| Язык | Kotlin / JVM 21 |
| Сборка | Gradle 8 (Kotlin DSL) |
| БД | PostgreSQL 16 + Flyway |
| Безопасность | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA + Hibernate |
| Документация | SpringDoc OpenAPI (`/swagger-ui.html`) |
| Тесты | JUnit 5 + Testcontainers |

## Запуск локально

```bash
# Поднять БД
docker compose up -d

# Запустить приложение (профиль dev, порт 8082)
./gradlew bootRun
```

Swagger UI: http://localhost:8082/swagger-ui.html  
Health: http://localhost:8082/actuator/health

## Тесты

```bash
./gradlew test
```

## Роли

| Роль | Права |
|------|-------|
| `ADMIN` | Полный доступ |
| `MANAGER` | Лиды, студенты, курсы, группы, расписание |
| `TEACHER` | Свои группы и занятия |
| `STUDENT` | Свой профиль, свои группы, своё расписание |

## Переменные окружения (prod)

| Переменная | Описание |
|-----------|---------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` | Подключение к БД |
| `DB_PASSWORD_FILE` | Путь к Docker secret с паролем БД |
| `JWT_SECRET_FILE` | Путь к Docker secret с JWT-ключом |
| `SERVER_PORT` | Порт сервера (по умолчанию 8080) |
