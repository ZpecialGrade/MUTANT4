
Тестить на винде — самый простой способ Swagger UI в браузере:

http://localhost:8081/swagger-ui.html — открой POST /auth/register, нажми Try it out, введи:


{"email":"a@b.c","password":"passw0rd1"}
Там же POST /auth/login с теми же данными → в ответе скопируй accessToken.

Сверху справа кнопка Authorize → вставь Bearer <вставь-токен> → Authorize.

Жми GET /auth/me — должно вернуть email+userId.

GET /auth/events/me — список auth-событий из Mongo. Если не пусто — мой фикс MongoConfig работает.

Переходи на http://localhost:8082/swagger-ui.html → Authorize → тот же Bearer <тот-же-токен>:

POST /profiles с {"displayName":"test"} → профиль в Postgres
POST /items — форма multipart: name=jeans, color=blue, type=BOTTOM, файл-картинка любой → Postgres + MinIO + Mongo activity_events
GET /items → должен вернуть только что созданный
MinIO UI: http://localhost:9001 (логин stylish / stylish123) → bucket stylish → увидишь залитую картинку.

Frontend: http://localhost:3000 — если фронт подключён к бэку, можно залогиниться и прокликать через него.