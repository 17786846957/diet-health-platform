# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

智能饮食健康管理平台 — full-stack diet/nutrition tracking app with food library, meal logging, family members, exercise/water/weight tracking, and admin dashboard.

## Commands

### Backend (`diet-health-backend/`)
```bash
mvn spring-boot:run                    # Start on port 8082
mvn test                               # Run 49 unit tests + JaCoCo coverage
mvn test -Dtest=UserServiceTest        # Run single test class
mvn test -Dtest=UserServiceTest#testMethod  # Run single test method
mvn package -DskipTests                # Build JAR
```

### Frontend (`diet-health-frontend/`)
```bash
npm run dev                            # Vite dev server on port 5173 (proxies /api → localhost:8082)
npm run build                          # Production build to dist/
npx playwright test                    # Run 33 E2E tests (Chromium)
npx playwright test --ui               # E2E with visual UI
npx playwright test e2e/auth.spec.js   # Run single E2E spec
```

### Database
```bash
mysql -u root -p < sql/init.sql                # Schema + 38 foods
mysql -u root -p diet_health < sql/food_expand.sql    # +193 foods
mysql -u root -p diet_health < sql/expand_tables.sql  # Extra tables
```

### Docker
```bash
docker-compose up -d                   # mysql + backend + frontend (Nginx on port 80)
JWT_SECRET=<secret> docker-compose up  # Production with custom JWT secret
```

## Architecture

### Backend (Spring Boot 2.7.18, Java 11)

Root package: `com.diet`. Layers: `controller/` → `service/` → `mapper/` (MyBatis-Plus). Entities in `entity/`, DTOs in `dto/`, config in `config/`.

**Auth flow:** JWT stored in httpOnly Cookie `diet_token` (SameSite=Lax). `JwtAuthenticationFilter` reads cookie → validates token → sets `SecurityContext`. Public: `/auth/*`. Admin-only: `/admin/**`, `/swagger-ui/**`.

**Key patterns:**
- All responses wrapped in `R<T>` (`{code, message, data}`)
- Business errors thrown as `BusinessException`, caught by `GlobalExceptionHandler`
- Rate limiting via `RateLimitFilter` (login 5/min, register 3/2min)
- `@AuditLog` AOP annotation for audit trails
- Constructor injection everywhere (no `@Autowired`)

### Frontend (Vue 3 + Vite + Element Plus)

`<script setup>` Composition API only. Pinia stores: `user.js` (auth + family), `app.js` (loading), `diet.js`.

**API layer:** `api/request.js` creates Axios instance with `withCredentials: true`. Response interceptor unwraps `R<T>` envelope, auto-redirects on 401. Each domain has its own API module (`api/auth.js`, `api/diet.js`, etc.).

**Routing:** Vue Router with lazy-loaded views. Auth guard checks `userStore.isLoggedIn`. Admin route requires `role === 'admin'`.

### Database (MySQL 8.0, `diet_health`)

Core tables: `user`, `food` (230+ items from 中国食物成分表), `diet_record` + `diet_record_detail` (1:N), `family_member`, `water_record`, `exercise_record`, `weight_record`, `health_goal`, `body_symptom`, `food_favorite`.

## Testing

- **Backend:** JUnit 5 + Mockito. Tests use H2 in-memory DB (MySQL mode). Integration test (`AuthIntegrationTest`) uses Testcontainers, gated by `CI=true` env var.
- **Frontend:** Playwright E2E only (no unit tests). Config in `playwright.config.js`, Chromium-only, auto-starts dev server.
- **Test accounts:** admin/admin123 (admin role), or register new user via UI.

## Environment Variables

| Variable | Default | Notes |
|----------|---------|-------|
| `DB_HOST` | `localhost` | `mysql` in Docker |
| `DB_USER` | `root` | |
| `DB_PASSWORD` | `123456` | |
| `JWT_SECRET` | dev fallback | **Required for prod** |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` disables rate-limit bypass |
