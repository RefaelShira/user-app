# 📦 User Management Application (Spring Boot + React)

## 🎯 Requirements
- Backend in Java Spring Boot + DB (H2).
- Frontend in React (TBD).
- Features:
    - Create user (POST).
    - List users with paging + search (GET).
    - Soft delete (DELETE).
    - Statistics: users created in last 24h (GET).
- Resilience: all DB operations wrapped with Retry + CircuitBreaker (Resilience4j).
- Unified responses with `ApiResponseDto<T>` using `ResponseBuilder`.
- Global error handling via `@RestControllerAdvice`.

---

## 📂 Backend Structure

### Entity
UserEntity with fields: id (UUID), firstName, lastName, email (unique), passwordHash, active (boolean), createdAt, updatedAt, version (optimistic locking).

### Repository
- `findByEmailIgnoreCase`
- `search(q, activeOnly, Pageable)` – query with filters.
- `countUsersCreatedSince(Instant since)` – for stats.

### DTOs
- CreateUserRequest
- UserResponse
- PagedResponse<T> + PageMeta
- UserStatsResponse
- ApiResponseDto<T>

### Utils
- ResponseBuilder: success / created / error with ApiResponseDto wrapper.

### Service
- `create(CreateUserRequest)` – validate, hash password, save.
- `list(...)` – returns Page<UserResponse>.
- `delete(id, soft)` – soft delete (409 if already inactive).
- `getStats()` – count users created in last 24h.

### Controller
- `POST /api/users` – create user.
- `GET /api/users` – list with paging + search.
- `DELETE /api/users/{id}?soft=true` – soft delete (2nd call -> 409).
- `GET /api/users/stats` – stats endpoint.

### Error Handling
Global `@RestControllerAdvice` returns ApiResponseDto with proper status codes:
- 400 validation errors
- 404 not found
- 409 conflict (duplicate email, already inactive)
- 503 service unavailable (circuit breaker / db issues)
- 500 generic error

---

## 🗃️ schema.sql

```sql
create table if not exists users (
    id uuid primary key,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(320) not null,
    password_hash varchar(100) not null,
    is_active boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null,
    version bigint not null
);

create unique index if not exists uq_users_email on users(email);
create index if not exists idx_users_created_at on users(created_at);
create index if not exists idx_users_is_active on users(is_active);
create index if not exists idx_users_active_created_at on users(is_active, created_at);
