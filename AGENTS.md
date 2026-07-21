## Build & Run

Source lives under `build/`:
- `build/backend` — Maven Spring Boot 3.2.2 (Java 17) single module, DDD packages
- `build/web` — Vue 3 + TypeScript + Vite + Element Plus admin UI

### Backend (built & run inside Docker — local env has only JDK8, no Maven)
Build image and run via compose:
```
docker compose build supervision-api
docker compose up -d mysql redis rabbitmq supervision-api
```
Compile/verify inside the maven builder container (equiv. of `mvn`):
```
docker compose run --rm maven mvn -B -q compile
docker compose run --rm maven mvn -B -q test
```

### Frontend
```
cd build/web && npm install
cd build/web && npm run dev        # dev server
cd build/web && npm run build      # production build -> dist/
```

## Validation
- Backend health: `curl -f http://localhost:8080/api/health`
- Frontend build: `cd build/web && npm run build` (must exit 0)
- Smoke test (after compose up): create task → execute → list executions

## Operational Notes
All source and config under `build/`. The canonical deployable artifact is produced by
`docker compose up -d` (mysql + redis + rabbitmq + supervision-api + nginx).
Local JDK is 8; the backend targets Java 17 and is compiled via the `maven:3.9-eclipse-temurin-17`
multi-stage image defined in `build/backend/Dockerfile`.
