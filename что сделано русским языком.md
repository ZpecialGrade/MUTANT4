# План по заданиям — что и где применяется

## 1. Docker + Swagger

**Docker** (уже есть): 7 контейнеров в `docker-compose.yml` — `postgres`, `minio` + `minio-init`, три Spring-микросервиса (`auth-service`, `wardrobe-service`, `generation-service`), `frontend` (React + nginx). Межсервисное общение через внутреннюю Docker-сеть; внешние порты: 3000 (фронт), 8081-8083 (бэки), 5432 (Postgres), 9000/9001 (MinIO).

**Swagger**: добавляется `springdoc-openapi-starter-webmvc-ui` во все три Spring-сервиса. Три отдельных Swagger UI:

- `localhost:8081/swagger-ui/index.html` — auth
- `localhost:8082/swagger-ui/index.html` — wardrobe
- `localhost:8083/swagger-ui/index.html` — generation

В `SecurityConfig` открываем пути `/swagger-ui/**` и `/v3/api-docs/**`. Ключевые эндпоинты получают `@Operation` / `@Tag` для читаемой документации.

---

## 2. Async + Cache + Testing

**Async** — `@EnableAsync` в `AuthServiceApplication` и `WardrobeServiceApplication`. Конфигурируется **`ThreadPoolTaskExecutor`** (класс `AsyncConfig`) с фиксированным core/max pool и именованными потоками. Применяется:

- `auth-service` — асинхронная запись `AuthEvent` в MongoDB на каждый login/refresh/logout
- `wardrobe-service` — асинхронная запись `ActivityEvent` при создании item-а, загрузке фото, генерации лука

Зачем: основной HTTP-запрос не ждёт, пока Mongo проглотит событие.

**Cache** — Spring Cache Abstraction + **Caffeine** (in-memory). `@EnableCaching` в обоих сервисах:

- `auth-service` — `@Cacheable` на лукап юзера по email (частые логины/рефреши не долбят Postgres)
- `wardrobe-service` — `@Cacheable` на `ProfileService.getProfileByUserId` (профиль читается на **каждом** запросе в wardrobe — главный хот-путь)
- `@CacheEvict` на мутирующих операциях

**Testing** — JUnit 5 + Mockito + **Testcontainers** (живой Postgres и Mongo в Docker при прогоне):

*Unit-тесты (сервисный слой, моки репозиториев):*

- `AuthServiceTest` — happy-path login, неверный пароль, дубликат email, отозванный/протухший refresh, disabled-юзер
- `ItemServiceTest` — create / get / delete, изоляция юзеров (чужое нельзя увидеть)
- `LookServiceTest` — валидация дубликатов типов, not-found, happy-path
- `ActivityServiceTest` / `AuthEventServiceTest` — `@Async` отрабатывает и пишет в Mongo

*Integration-тесты (`@SpringBootTest` + Testcontainers):*

- `AuthControllerIT` — полный флоу: register → login → /me → refresh → logout
- `ItemControllerIT` — multipart upload, список, удаление, 401 без токена

*Web-slice (`@WebMvcTest`)* — проверка security-цепочки (401/403 на закрытых путях).

---

## 3. File upload + File download

В `wardrobe-service`. Хранилище — **MinIO** (S3-совместимое), через AWS SDK v2 (`S3Client`).

**Upload:**

- `POST /items` — multipart/form-data с `metadata` (JSON) + `file` (картинка вещи)
- `POST /user-photos` — multipart с `file` (фото пользователя)
- Сервис генерит object key `items/{profileId}/{uuid}.{ext}`, стримит файл в MinIO, в Postgres — только метаданные + ключ

**Download:**

- `GET /files/{objectKey}` — `FileController` стримит файл из MinIO наружу
- Используется фронтом для `<img src="/files/...">`
- Бакет MinIO приватный — все файлы проходят через сервис (MinIO снаружи не доступен анонимно после наших фиксов)

В Swagger multipart-эндпоинты оформлены так, что файл можно загрузить прямо из UI.

---

## 4. Spring Security + JWT

**auth-service (issuer — выпускает токены):**

- `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server`
- **HS256**, shared secret из env `JWT_ACCESS_SECRET`
- `NimbusJwtEncoder` выпускает access-токен (claims: `sub`=userId, `email`, `iat`, `exp`)
- Access TTL 15 минут, Refresh TTL 30 дней
- **Refresh-токены с ротацией**: формат `{uuid}.{secret}`, `secret` хранится BCrypt-хэшем; на refresh старый токен `revoked_at`, выдаётся новый
- Пароли — BCrypt
- Открытые: `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`
- Закрытые: `/auth/me` (требует JWT), `/auth/events/me` (новое, про Mongo — п.5)

**wardrobe-service (resource server — валидирует):**

- `NimbusJwtDecoder` с тем же HS256-секретом → валидация локально, без хождения в auth-service
- `.oauth2ResourceServer().jwt()` — автоматически парсит `Bearer`-токен, кладёт `Jwt` в `SecurityContext`
- `CurrentUser.userId()` достаёт `sub` из JWT
- **Ownership на уровне БД**: все запросы фильтруются по `profile_id` текущего юзера — через `findByIdAndProfile_Id(...)`, чужое достать нельзя

**Общее:** `SessionCreationPolicy.STATELESS`, CSRF выключен (не нужен с Bearer-токенами).

---

## 5. MongoDB

Один контейнер `mongo` с двумя базами (как с Postgres — одна инстанция, разные schema):

- `authdb` → `auth-service`
- `wardrobedb` → `wardrobe-service`

`spring-boot-starter-data-mongodb` в обоих сервисах.

**auth-service — audit log безопасности.** Коллекция `auth_events`, документ `AuthEvent`:

- `userId` (nullable — для failed login по неизвестному email)
- `email`, `type` (`REGISTER` / `LOGIN_SUCCESS` / `LOGIN_FAILED` / `REFRESH_SUCCESS` / `REFRESH_FAILED` / `LOGOUT`)
- `ipAddress`, `userAgent` (из `HttpServletRequest`)
- `createdAt`
- Пишется **асинхронно** (`@Async`) на каждом auth-флоу
- Новый эндпоинт `GET /auth/events/me` — юзер видит свою историю входов ("меня взломали? посмотрю")

**wardrobe-service — activity feed.** Коллекция `activity_events`, документ `ActivityEvent`:

- `userId`, `type` (`ITEM_CREATED` / `ITEM_DELETED` / `LOOK_GENERATED` / `USER_PHOTO_UPLOADED`)
- `payload` (JSON с `itemId` / `lookId` и т.д.)
- `createdAt`
- Пишется **асинхронно** при соответствующих действиях
- Новый эндпоинт `GET /activities/me` — последние N событий юзера

---

## Сводка "где искать какую технологию"

| Требование | Где применяется |
|---|---|
| Docker | `docker-compose.yml` (7 сервисов) |
| Swagger | все три Spring-сервиса, порты 8081/8082/8083 `/swagger-ui` |
| Async | `auth-service` + `wardrobe-service` — запись в Mongo |
| Cache | `auth-service` (user by email) + `wardrobe-service` (profile by userId), Caffeine |
| Testing | `*Test.java` (unit) + `*IT.java` (Testcontainers) в обоих сервисах |
| File upload | `POST /items`, `POST /user-photos` (multipart, MinIO) |
| File download | `GET /files/{key}` → MinIO |
| Spring Security | оба сервиса, `SecurityConfig.java` |
| JWT | `auth-service` выпускает (HS256), `wardrobe-service` валидирует (shared secret) |
| MongoDB | `auth-service` — auth audit, `wardrobe-service` — activity feed |
