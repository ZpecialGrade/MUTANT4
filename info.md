# Реализация требований — технический обзор

Проект: multi-module Maven, parent [pom.xml](pom.xml), Spring Boot 4.0.3, Java 17. Три Spring-микросервиса (`auth-service`, `wardrobe-service`, `generation-service`) + React-фронт.

---

## 1. Docker + Swagger

### Docker

Оркестрация в [docker-compose.yml](docker-compose.yml). Сервисы:

| Контейнер | Образ | Назначение | Внешний порт |
|---|---|---|---|
| `postgres` | `postgres:17` | PostgreSQL, схемы `auth` и `wardrobe` | 5432 |
| `mongo` | `mongo:7` | БД `authdb` и `wardrobedb` | 27017 |
| `minio` | `minio:RELEASE.2025-02-28T...` | S3-совместимое хранилище | 9000 (API) / 9001 (console) |
| `minio-init` | `minio/mc` | Создание бакета `stylish` при старте | — |
| `auth-service` | build | Сервис аутентификации | 8081 |
| `wardrobe-service` | build | Основной бизнес-сервис | 8082 |
| `generation-service` | build | Stub для ML-генерации | 8083 |
| `frontend` | build (React+nginx) | UI | 3000 |

Межсервисная связь — внутренняя Docker-сеть (DNS по именам сервисов). Healthchecks для `postgres`, `mongo`, `minio`, зависимости через `depends_on: condition: service_healthy`.

Dockerfile паттерн — multi-stage build, `maven:3.9.9-eclipse-temurin-17 → eclipse-temurin:17-jre`: [services/auth-service/Dockerfile](services/auth-service/Dockerfile), [services/wardrobe-service/Dockerfile](services/wardrobe-service/Dockerfile), [services/generation-service/Dockerfile](services/generation-service/Dockerfile).

### Swagger

Зависимость `springdoc-openapi-starter-webmvc-ui` (v2.7.0) в POM каждого сервиса. Три независимых Swagger UI:

- http://localhost:8081/swagger-ui.html — auth
- http://localhost:8082/swagger-ui.html — wardrobe
- http://localhost:8083/swagger-ui.html — generation

Конфигурация `OpenAPI` bean с описанием security scheme `bearer` — [services/auth-service/src/main/java/com/stylish/auth/config/OpenApiConfig.java](services/auth-service/src/main/java/com/stylish/auth/config/OpenApiConfig.java), аналогично в wardrobe и generation.

В `SecurityConfig` открыты пути `/swagger-ui/**`, `/v3/api-docs/**` — [SecurityConfig.java:59](services/auth-service/src/main/java/com/stylish/auth/config/SecurityConfig.java#L59), [SecurityConfig.java:43](services/wardrobe-service/src/main/java/com/stylish/wardrobe/config/SecurityConfig.java#L43).

Эндпоинты аннотированы `@Operation` / `@Tag`, защищённые — `@SecurityRequirement(name = "bearer")`.

---

## 2. Async + Cache

### Async

`@EnableAsync` на главных классах:
- [AuthServiceApplication.java:10](services/auth-service/src/main/java/com/stylish/auth/AuthServiceApplication.java#L10)
- [WardrobeServiceApplication.java:10](services/wardrobe-service/src/main/java/com/stylish/wardrobe/WardrobeServiceApplication.java#L10)

Thread pool executor — `ThreadPoolTaskExecutor` с именем бина `auditExecutor`, core=2 / max=4 / queue=200:
- [auth-service AsyncConfig.java](services/auth-service/src/main/java/com/stylish/auth/config/AsyncConfig.java)
- [wardrobe-service AsyncConfig.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/config/AsyncConfig.java)

Применение:
- `AuthEventService.record(...)` — `@Async("auditExecutor")`, запись `AuthEvent` в Mongo при каждом register / login / refresh / logout — [AuthEventService.java](services/auth-service/src/main/java/com/stylish/auth/event/AuthEventService.java)
- `ActivityService.record(...)` — `@Async("auditExecutor")`, запись `ActivityEvent` при создании/обновлении/удалении item, загрузке user-photo, генерации look — [ActivityService.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/activity/ActivityService.java)

Цель: HTTP-запрос не блокируется на записи аудита в Mongo.

### Cache

Spring Cache Abstraction + Caffeine (in-memory, TTL 10 мин, max 10 000 записей). `@EnableCaching` в `CacheConfig`:
- [auth-service CacheConfig.java](services/auth-service/src/main/java/com/stylish/auth/config/CacheConfig.java) — кэш `USERS_BY_EMAIL`
- [wardrobe-service CacheConfig.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/config/CacheConfig.java) — кэш `profilesByUserId`

Применение:
- **auth-service**: [UserLookupService.java:18](services/auth-service/src/main/java/com/stylish/auth/user/UserLookupService.java#L18) — `@Cacheable(USERS_BY_EMAIL, key = "#email")` на лукап юзера по email (каждый логин/рефреш). `@CacheEvict` на обновлениях профиля.
- **wardrobe-service**: [ProfileService.java:38](services/wardrobe-service/src/main/java/com/stylish/wardrobe/profile/ProfileService.java#L38) — `@Cacheable(PROFILES_BY_USER_ID, key = "#userId")` на `getProfileByUserId`. Профиль читается на каждом защищённом запросе — главный хот-путь. `@CacheEvict` на `createProfile` — [ProfileService.java:27](services/wardrobe-service/src/main/java/com/stylish/wardrobe/profile/ProfileService.java#L27).

---

## 3. Testing

JUnit 5 + Mockito, unit-тесты на сервисный слой (репозитории замокированы):

**auth-service** ([src/test/java/com/stylish/auth/](services/auth-service/src/test/java/com/stylish/auth)):
- `AuthServiceTest` — happy-path login, invalid password, duplicate email, revoked/expired refresh, disabled user
- `AuthEventServiceTest` — проверка асинхронной записи событий в Mongo

**wardrobe-service** ([src/test/java/com/stylish/wardrobe/](services/wardrobe-service/src/test/java/com/stylish/wardrobe)):
- `ProfileServiceTest` — создание профиля, дубликат, чтение
- `ItemServiceTest` — CRUD, изоляция по `profileId` (ownership)
- `LookServiceTest` — валидация, not-found, happy-path генерации лука
- `ActivityServiceTest` — проверка `@Async`-записи активности

> Интеграционные тесты с Testcontainers и `@SpringBootTest` — не реализованы. При необходимости добавляются отдельно, структура проекта готова.

---

## 4. File upload / download

Локация — `wardrobe-service`, пакет `storage`. AWS SDK v2 (`software.amazon.awssdk:s3`), бакет приватный в MinIO.

Абстракция хранилища — `StorageService` ([services/wardrobe-service/src/main/java/com/stylish/wardrobe/storage/StorageService.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/storage/StorageService.java)): `putObject`, `getObject`, `deleteObject`.

### Upload

Все multipart-эндпоинты используют `MediaType.MULTIPART_FORM_DATA_VALUE` + `@RequestPart`:

- `POST /items` — [ItemController.java:45](services/wardrobe-service/src/main/java/com/stylish/wardrobe/item/ItemController.java#L45)
  Части: `metadata` (JSON `CreateItemMetadata`) + `file` (картинка вещи). Сервис генерирует object key `items/{profileId}/{uuid}.{ext}`, стримит файл в S3, в Postgres кладёт только метаданные + ключ.
- `POST /user-photos` — [UserPhotoController.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/photo/UserPhotoController.java), часть `file`.

### Download

`GET /files/{*objectKey}` — [FileController.java:24](services/wardrobe-service/src/main/java/com/stylish/wardrobe/storage/FileController.java#L24). Стримит файл из MinIO через `InputStreamResource` с корректным `Content-Type` и `Content-Length`. Используется фронтом для `<img src="/files/...">`.

Бакет MinIO приватный — внешнего анонимного доступа нет, все файлы идут через сервис.

---

## 5. Spring Security + JWT

### auth-service (issuer)

Стартеры: `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` (auth-service тоже валидирует JWT для `/auth/me`).

Конфигурация — [SecurityConfig.java](services/auth-service/src/main/java/com/stylish/auth/config/SecurityConfig.java):
- Алгоритм **HS256**, shared secret из env `JWT_ACCESS_SECRET`.
- `NimbusJwtEncoder` (`ImmutableSecret`) — выпуск access-токена. Claims: `sub` = `userId` (UUID), `email`, `iat`, `exp`.
- `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)` — валидация.
- `BCryptPasswordEncoder` — хэширование паролей.
- `SessionCreationPolicy.STATELESS`, CSRF выключен.

TTL — в `application.yml` и `JwtProperties` ([JwtProperties.java](services/auth-service/src/main/java/com/stylish/auth/config/JwtProperties.java)): access 15 минут (`PT15M`), refresh 30 дней (`P30D`).

**Refresh-токены с ротацией**: формат `{uuid}.{secret}`, в БД хранится только `bcrypt(secret)` (таблица `refresh_tokens` в миграции [V1__init.sql](services/auth-service/src/main/resources/db/migration/V1__init.sql)). На `POST /auth/refresh` старый токен помечается `revoked_at`, выпускается новая пара.

Эндпоинты — [AuthController.java](services/auth-service/src/main/java/com/stylish/auth/controller/AuthController.java):

| Путь | Метод | Доступ |
|---|---|---|
| `/auth/register` | POST | публичный |
| `/auth/login` | POST | публичный |
| `/auth/refresh` | POST | публичный (по refresh-токену) |
| `/auth/logout` | POST | публичный (по refresh-токену) |
| `/auth/me` | GET | Bearer JWT |
| `/auth/events/me` | GET | Bearer JWT |

### wardrobe-service (resource server)

[SecurityConfig.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/config/SecurityConfig.java):
- Тот же HS256 + shared secret → `NimbusJwtDecoder` валидирует локально, без похода в auth-service.
- `.oauth2ResourceServer().jwt()` — автоматический парсинг `Bearer`-заголовка, кладёт `Jwt` в `SecurityContext`.
- [CurrentUser.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/security/CurrentUser.java) — helper, достаёт `sub` → `UUID userId`.

**Ownership на уровне БД**: репозитории используют named queries типа `findByIdAndProfile_Id(id, profileId)` ([ItemRepository.java:12](services/wardrobe-service/src/main/java/com/stylish/wardrobe/item/ItemRepository.java#L12)) — чужие данные недостижимы.

---

## 6. MongoDB

Один контейнер `mongo`, два логических БД:

| БД | Сервис | Коллекции |
|---|---|---|
| `authdb` | auth-service | `auth_events` |
| `wardrobedb` | wardrobe-service | `activity_events` |

Зависимость — `spring-boot-starter-data-mongodb`. URI берётся из env `MONGO_URI` через явный бин `MongoClient` (обход автоконфига Spring Boot 4, который в текущей версии некорректно резолвит `spring.data.mongodb.uri` из yml):
- [auth-service MongoConfig.java](services/auth-service/src/main/java/com/stylish/auth/config/MongoConfig.java)
- [wardrobe-service MongoConfig.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/config/MongoConfig.java)

Оба класса extend `AbstractMongoClientConfiguration`, переопределяют `mongoClient()`, `getDatabaseName()` (из `ConnectionString`), `getMappingBasePackages()` (корневой пакет сервиса для сканирования `@Document`).

### auth-service — audit log безопасности

Документ [AuthEvent.java](services/auth-service/src/main/java/com/stylish/auth/event/AuthEvent.java), коллекция `auth_events`. Поля:
- `id` (ObjectId as String)
- `userId` (UUID, `@Indexed`, nullable — для failed login по неизвестному email)
- `email` (`@Indexed`)
- `type` — enum [AuthEventType](services/auth-service/src/main/java/com/stylish/auth/event/AuthEventType.java): `REGISTER`, `LOGIN_SUCCESS`, `LOGIN_FAILED`, `REFRESH`, `LOGOUT`
- `ipAddress`, `userAgent` — извлекаются из `HttpServletRequest` в контроллере ([AuthController.java:63](services/auth-service/src/main/java/com/stylish/auth/controller/AuthController.java#L63))
- `createdAt` (`@Indexed`)

Запись асинхронная, сервис — [AuthEventService.java](services/auth-service/src/main/java/com/stylish/auth/event/AuthEventService.java), репозиторий `MongoRepository<AuthEvent, String>` — [AuthEventRepository.java](services/auth-service/src/main/java/com/stylish/auth/event/AuthEventRepository.java).

Эндпоинт — `GET /auth/events/me` ([AuthEventController.java](services/auth-service/src/main/java/com/stylish/auth/event/AuthEventController.java)) — история входов текущего пользователя.

### wardrobe-service — activity feed

Документ [ActivityEvent.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/activity/ActivityEvent.java), коллекция `activity_events`. Поля:
- `id`, `userId` (`@Indexed`), `targetId` (UUID сущности: item/look/photo)
- `type` — enum [ActivityType](services/wardrobe-service/src/main/java/com/stylish/wardrobe/activity/ActivityType.java): `PROFILE_CREATED`, `ITEM_CREATED`, `ITEM_UPDATED`, `ITEM_DELETED`, `USER_PHOTO_UPLOADED`, `LOOK_GENERATED`, `LOOK_RENAMED`
- `description` (человекочитаемая сводка)
- `createdAt` (`@Indexed`)

Запись асинхронная — [ActivityService.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/activity/ActivityService.java).

Эндпоинт — `GET /activities/me` ([ActivityController.java:30](services/wardrobe-service/src/main/java/com/stylish/wardrobe/activity/ActivityController.java#L30)) — последние N событий пользователя (параметр `limit`, default 50).

---

## 7. Flyway (PostgreSQL миграции)

Аналогично Mongo — автоконфиг Spring Boot 4 не всегда корректно стартует Flyway перед Hibernate-валидацией. Сделано явно через бин `Flyway` с `initMethod = "migrate"` + `BeanFactoryPostProcessor`, добавляющий `@DependsOn("flyway")` на `EntityManagerFactory`:
- [auth-service FlywayConfig.java](services/auth-service/src/main/java/com/stylish/auth/config/FlywayConfig.java) (схема `auth`)
- [wardrobe-service FlywayConfig.java](services/wardrobe-service/src/main/java/com/stylish/wardrobe/config/FlywayConfig.java) (схема `wardrobe`)

Миграции:
- [services/auth-service/src/main/resources/db/migration/V1__init.sql](services/auth-service/src/main/resources/db/migration/V1__init.sql) — `users`, `refresh_tokens`
- [services/wardrobe-service/src/main/resources/db/migration/V1__init.sql](services/wardrobe-service/src/main/resources/db/migration/V1__init.sql) — `profiles`, `items`, `user_photos`, `looks`, `look_items`

---

## Сводка: где искать какую технологию

| Требование | Файлы / локации |
|---|---|
| Docker | [docker-compose.yml](docker-compose.yml), `services/*/Dockerfile` |
| Swagger | `OpenApiConfig.java` в каждом сервисе, `/swagger-ui.html` на портах 8081/8082/8083 |
| Async | `AsyncConfig.java` + `@Async` на `AuthEventService` / `ActivityService` |
| Cache | `CacheConfig.java` + `@Cacheable` в `UserLookupService` (auth) и `ProfileService` (wardrobe) |
| Testing | `services/*/src/test/java/...` — `*ServiceTest.java` (JUnit 5 + Mockito) |
| File upload | `POST /items`, `POST /user-photos` (multipart), `StorageService` |
| File download | `GET /files/{objectKey}` — `FileController` |
| Spring Security | `SecurityConfig.java` в `auth-service` и `wardrobe-service` |
| JWT | `SecurityConfig.java` + `JwtProperties.java` в auth (issuer), `SecurityConfig.java` в wardrobe (resource server) |
| MongoDB | `MongoConfig.java` + `AuthEvent` / `ActivityEvent` документы |
| Flyway | `FlywayConfig.java` + `db/migration/V1__init.sql` |
