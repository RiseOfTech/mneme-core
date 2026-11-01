# cross-origin-access Specification

## Purpose

Позволяет браузерному клиенту с другого origin вызывать API, не открывая production-сервис для всех
origin.

## Requirements

### Requirement: Только настроенные origin

Система SHALL разрешать кросс-доменный доступ к `/v1/**` только с настроенных origin. Конфигурация
по умолчанию разрешает `http://localhost:5173`; профиль `prod` MUST не разрешать ни один origin,
если он не настроен. Разрешёнными методами SHALL быть `GET`, `POST`, `PUT`, `DELETE` и `OPTIONS`, а
разрешёнными заголовками запроса SHALL быть `Authorization` и `Content-Type`. Credentials
MUST NOT быть включены.

#### Scenario: Preflight с разрешённого origin
- **WHEN** разрешённый origin отправляет preflight `OPTIONS` без токена
- **THEN** запрос успешен и содержит разрешённые origin, методы и заголовки

#### Scenario: Запрещённый origin
- **WHEN** origin, который не настроен, отправляет preflight
- **THEN** ответ не разрешает этот origin

#### Scenario: Аутентифицированный кросс-доменный запрос
- **WHEN** разрешённый origin отправляет реальный запрос с действительным токеном
- **THEN** запрос успешен и содержит заголовки ответа CORS

#### Scenario: Origin не настроены
- **WHEN** ни один origin не настроен
- **THEN** ни один origin не разрешён
