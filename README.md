# User App (Monorepo)

## Backend (Spring Boot)
- Run: `mvn spring-boot:run`
- Base URL: `http://localhost:9090`
- Swagger: `http://localhost:9090/swagger-ui/index.html`

## Frontend (Vite + React + Tailwind)
- Node 20.19+ או 22.12+
- Run:
  - `cd frontend && npm i`
  - `npm run dev` → `http://localhost:5173`

## Dev Proxy
`frontend/vite.config.ts` `/api` → `http://localhost:9090`
