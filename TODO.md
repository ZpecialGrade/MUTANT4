# TODO — этапы внедрения

Порядок выбран так, чтобы каждый шаг опирался на предыдущий и ничего не ломал по пути.

---

## [x] Этап 1 — Infra

Добавить контейнер `mongo` в `docker-compose.yml` (одна инстанция, две базы: `authdb`, `wardrobedb`). Healthcheck. Проверить что всё поднимается.

**Файлы:**
- `docker-compose.yml`

---

## [x] Этап 2 — Swagger

`springdoc-openapi-starter-webmvc-ui` в `auth-service`, `wardrobe-service`, `generation-service`. Открыть `/swagger-ui/**` и `/v3/api-docs/**` в обоих `SecurityConfig`. Аннотации `@Tag`, `@Operation` на ключевых контроллерах (auth, items, user-photos, looks, files). Проверить три UI в браузере.

**Файлы:**
- `services/auth-service/pom.xml`
- `services/wardrobe-service/pom.xml`
- `services/generation-service/pom.xml`
- `services/auth-service/src/.../config/SecurityConfig.java`
- `services/wardrobe-service/src/.../config/SecurityConfig.java`
- контроллеры обоих сервисов — аннотации

---

## [x] Этап 3 — Mongo + Async в auth-service

Зависимость `spring-boot-starter-data-mongodb`. Класс `AsyncConfig` с `ThreadPoolTaskExecutor`. `@EnableAsync` в `AuthServiceApplication`. Документ `AuthEvent` + `AuthEventRepository`. `AuthEventService.record(...)` с `@Async`. Вызовы из `AuthService` и `AuthController` (прокинуть `HttpServletRequest` для `ipAddress` / `userAgent`). Эндпоинт `GET /auth/events/me`.

**Файлы (новые + существующие):**
- `services/auth-service/pom.xml`
- `services/auth-service/src/main/resources/application.yml` (mongo config)
- `services/auth-service/src/.../config/AsyncConfig.java` (новый)
- `services/auth-service/src/.../AuthServiceApplication.java` — `@EnableAsync`
- `services/auth-service/src/.../event/AuthEvent.java` (новый)
- `services/auth-service/src/.../event/AuthEventType.java` (новый)
- `services/auth-service/src/.../event/AuthEventRepository.java` (новый)
- `services/auth-service/src/.../event/AuthEventService.java` (новый)
- `services/auth-service/src/.../event/AuthEventController.java` (новый) — `GET /auth/events/me`
- `services/auth-service/src/.../service/AuthService.java` — вызовы `record(...)`
- `services/auth-service/src/.../controller/AuthController.java` — проброс `HttpServletRequest`
- `docker-compose.yml` — `MONGO_URI` env для сервиса

---

## [x] Этап 4 — Mongo + Async в wardrobe-service

То же самое, что в этапе 3, только `ActivityEvent`. Зависимость, `AsyncConfig`, `@EnableAsync`. Документ `ActivityEvent` + репа + `ActivityService` с `@Async`. Вызовы из `ItemService`, `UserPhotoService`, `LookService` на соответствующих действиях (`ITEM_CREATED`, `ITEM_DELETED`, `LOOK_GENERATED`, `USER_PHOTO_UPLOADED`). Эндпоинт `GET /activities/me`.

**Файлы (новые + существующие):**
- `services/wardrobe-service/pom.xml`
- `services/wardrobe-service/src/main/resources/application.yml`
- `services/wardrobe-service/src/.../config/AsyncConfig.java` (новый)
- `services/wardrobe-service/src/.../WardrobeServiceApplication.java` — `@EnableAsync`
- `services/wardrobe-service/src/.../activity/ActivityEvent.java` (новый)
- `services/wardrobe-service/src/.../activity/ActivityType.java` (новый)
- `services/wardrobe-service/src/.../activity/ActivityRepository.java` (новый)
- `services/wardrobe-service/src/.../activity/ActivityService.java` (новый)
- `services/wardrobe-service/src/.../activity/ActivityController.java` (новый)
- `services/wardrobe-service/src/.../item/ItemService.java` — вызовы record
- `services/wardrobe-service/src/.../photo/UserPhotoService.java`
- `services/wardrobe-service/src/.../look/LookService.java`
- `docker-compose.yml` — `MONGO_URI` env

---

## [x] Этап 5 — Cache (Caffeine)

`spring-boot-starter-cache` + `caffeine` в оба сервиса. `CacheConfig` с `CaffeineCacheManager` (TTL + размер). `@EnableCaching`.

- **auth**: `@Cacheable` на лукап юзера по email (через сервисный метод), `@CacheEvict` на `register`
- **wardrobe**: `@Cacheable` на `ProfileService.getProfileByUserId`, `@CacheEvict` на `createProfile`

**Файлы:**
- `services/auth-service/pom.xml`
- `services/wardrobe-service/pom.xml`
- `services/auth-service/src/.../config/CacheConfig.java` (новый)
- `services/wardrobe-service/src/.../config/CacheConfig.java` (новый)
- `services/auth-service/src/.../AuthServiceApplication.java` — `@EnableCaching`
- `services/wardrobe-service/src/.../WardrobeServiceApplication.java` — `@EnableCaching`
- `services/auth-service/src/.../service/AuthService.java` (или отдельный `UserLookupService`) — `@Cacheable`
- `services/wardrobe-service/src/.../profile/ProfileService.java` — `@Cacheable` / `@CacheEvict`

---

## [x] Этап 6 — Testing (unit)

Testcontainers + JUnit 5 + Mockito.

**Unit (моки репо):**

- `AuthServiceTest` — register / login (неверный пароль, duplicate email, disabled) / refresh (revoked, expired, disabled) / logout
- `ItemServiceTest` — create / get / delete / изоляция юзеров
- `LookServiceTest` — валидация дубликатов типов, not-found, happy-path
- `AuthEventServiceTest` — `@Async` отрабатывает, пишет в Mongo
- `ActivityServiceTest` — то же для wardrobe

**Integration (`@SpringBootTest` + Testcontainers Postgres + Mongo):**

- `AuthControllerIT` — register → login → /me → refresh → logout
- `ItemControllerIT` — multipart upload, list, delete, 401 без токена

**WebMvc-slice (`@WebMvcTest`):**

- Проверка security: 401 на закрытых эндпоинтах, 200 на `/actuator/health` и `/swagger-ui`

**Файлы:**
- `services/auth-service/pom.xml` — `testcontainers` deps
- `services/wardrobe-service/pom.xml` — то же
- `services/auth-service/src/test/java/...` — тесты
- `services/wardrobe-service/src/test/java/...` — тесты
