# CineCat

Catàleg de pel·lícules amb valoracions del públic, **web (Vue 3) + mòbil (Kotlin Multiplatform)** sobre una **API Go + PostgreSQL**, desplegat a **Railway**. Projecte d'aprenentatge: prioritzem la **claredat** sobre la completesa.

> Documentació de disseny (font de veritat): [`docs/`](docs/) — comença per [docs/README.md](docs/README.md), el [pla per fases](docs/PLA-IMPLEMENTACIO.md) i l'[especificació](docs/ESPECIFICACIO.md).

## Estructura del monorepo

```
cinecat/
├── backend/   ← API Go (l'única part amb codi ara mateix)
├── web/       ← frontend Vue 3   (placeholder, Fase 3)
├── mobile/    ← client KMP        (placeholder, Fase 5)
└── docs/      ← disseny i pla d'implementació
```

## Estat actual — Fase 1

El backend té la **base de dades** (3 taules: `users`, `movies`, `ratings`) i l'**API del catàleg**: CRUD de pel·lícules a `/api/movies`, amb la fitxa calculant `avg_score` i `rating_count` per SQL. Encara **no hi ha imatges ni auth** (els endpoints d'escriptura queden oberts fins a la Fase 4).

## Arrencar el backend en local

El servidor necessita dues variables d'entorn:

- `DATABASE_URL` (obligatòria): cadena de connexió a PostgreSQL.
- `PORT` (opcional, per defecte `8080`).

En arrencar, **aplica automàticament les migracions** pendents de `backend/migrations/`.

### 1. Una base de dades PostgreSQL

La manera més ràpida és un contenidor d'usar i llençar:

```bash
docker run -d --name cinecat-pg -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=cinecat -p 5432:5432 postgres:16-alpine
```

### 2a. El servidor amb Go instal·lat

```bash
cd backend
DATABASE_URL='postgres://postgres:dev@localhost:5432/cinecat?sslmode=disable' go run ./cmd/server
```

### 2b. El servidor amb Docker (no cal tenir Go instal·lat)

Fem servir el **mateix Dockerfile** que Railway, així el que funciona en local funciona desplegat.

```bash
cd backend
docker build -t cinecat-backend .
docker run --rm -p 8080:8080 -e DATABASE_URL='postgres://postgres:dev@host.docker.internal:5432/cinecat?sslmode=disable' cinecat-backend
```

### 3. Dades de prova i comprovació

```bash
cd backend
API_URL=http://localhost:8080 ./scripts/seed.sh   # 7 pel·lícules del disseny, via POST
curl http://localhost:8080/health                  # {"status":"ok"}
curl 'http://localhost:8080/api/movies?genre=Drama'
curl 'http://localhost:8080/api/movies?q=nits'
```

## Desplegament a Railway

El backend es desplega a Railway des de la CLI amb el seu `Dockerfile`. La configuració del build i el healthcheck (`/health`) viuen a [`backend/railway.json`](backend/railway.json). El detall de les ordres usades es documenta a la descripció del PR de cada fase.

Variables d'entorn rellevants per fase: a la **Fase 1** calen `PORT` (que Railway injecta) i `DATABASE_URL` (referenciada al plugin Postgres: `${{Postgres.DATABASE_URL}}`). Més endavant: `JWT_SECRET`, `UPLOAD_DIR`, `MAX_UPLOAD_MB` (veure [ESPECIFICACIO.md §9](docs/ESPECIFICACIO.md#9-pla-de-desplegament-a-railway)).
