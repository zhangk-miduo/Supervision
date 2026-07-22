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
Compile and test through the backend Docker build (the Dockerfile runs Maven tests):
```
docker compose build api
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

## Conversation Documentation

- Every conversation concerning this repository must be summarized into the project documentation under `doc/` before the task is considered complete.
- The summary must capture, as applicable: user requirements, conclusions and decisions, important evidence, affected modules, risks and open questions, and recommended or completed next actions.
- Update an existing relevant document when one exists; otherwise create a document using the naming and structure rules in `doc/rule.supervision.conversation-documentation.md`.
- Do not treat chat history as the only source of truth. Important corrections or changed decisions must be reflected in the project document during the same task.
- Purely social messages or conversations unrelated to this repository do not require documentation.
