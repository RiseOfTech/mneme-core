# api-documentation Specification

## Purpose

Публикует машиночитаемый контракт REST API для клиентских команд и браузерный интерфейс для его
проверки вне production.

## Requirements

### Requirement: Опубликованный контракт

Вне профиля `prod` система SHALL отдавать документ OpenAPI по адресу `/v3/api-docs` и Swagger UI.
Документ SHALL перечислять все эндпоинты `/v1` и объявлять схему безопасности HTTP bearer, показывая
ограничения валидации на телах запросов.

#### Scenario: Контракт отдаётся
- **WHEN** вне `prod` запрашивается `GET /v3/api-docs`
- **THEN** ответ — `200` с документом OpenAPI

#### Scenario: Swagger UI отдаётся
- **WHEN** вне `prod` запрашивается `GET /swagger-ui/index.html`
- **THEN** ответ — `200`

### Requirement: Нет документации в production

Профиль `prod` MUST отключать документ OpenAPI и Swagger UI.

#### Scenario: Профиль production
- **WHEN** сервис работает с профилем `prod`
- **THEN** документ OpenAPI и Swagger UI не отдаются
