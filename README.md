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

## Estat actual — Fase 2

El backend té la **base de dades** (3 taules: `users`, `movies`, `ratings`), l'**API del catàleg** (CRUD a `/api/movies`, amb `avg_score` i `rating_count` calculats per SQL) i els **pòsters**: es pugen a `POST /api/movies/{id}/poster`, es desen redimensionats al disc (`UPLOAD_DIR`) i se serveixen a `/uploads/posters/<id>.jpg`. Encara **no hi ha auth** (els endpoints d'escriptura queden oberts fins a la Fase 4).

## Arrencar el backend en local

Variables d'entorn:

- `DATABASE_URL` (obligatòria): cadena de connexió a PostgreSQL.
- `PORT` (opcional, per defecte `8080`).
- `UPLOAD_DIR` (opcional, per defecte `uploads`, relatiu a on s'executa: `backend/uploads` amb `go run`). Carpeta on es desen els pòsters; es crea sola.
- `MAX_UPLOAD_MB` (opcional, per defecte `5`). Mida màxima d'un pòster.

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

### 4. Pujar un pòster

```bash
curl -F "file=@poster.jpg" http://localhost:8080/api/movies/<id>/poster
# {"poster_url":"/uploads/posters/<id>.jpg"}  → obre http://localhost:8080/uploads/posters/<id>.jpg
```

Només s'accepten JPG i PNG (comprovats pel contingut, no per l'extensió → `415`) de fins a `MAX_UPLOAD_MB` (→ `413`). El servidor el redimensiona a 500px d'amplada com a màxim i el desa sempre com a JPEG.

## Desplegament a Railway

El backend es desplega a Railway des de la CLI amb el seu `Dockerfile`. La configuració del build i el healthcheck (`/health`) viuen a [`backend/railway.json`](backend/railway.json). El detall de les ordres usades es documenta a la descripció del PR de cada fase.

Variables d'entorn a Railway: `PORT` (la injecta Railway), `DATABASE_URL` (referenciada al plugin Postgres: `${{Postgres.DATABASE_URL}}`) i `UPLOAD_DIR=/app/uploads`. Més endavant: `JWT_SECRET` (veure [ESPECIFICACIO.md §9](docs/ESPECIFICACIO.md#9-pla-de-desplegament-a-railway)).

### Volum per als pòsters

El disc d'un contenidor **s'esborra a cada desplegament**. Perquè els pòsters sobrevisquin, el servei `backend` necessita un **volum persistent** muntat a `/app/uploads` (el `WORKDIR` de la imatge és `/app`):

```bash
railway volume --service backend add --mount-path /app/uploads
railway variables --service backend --set UPLOAD_DIR=/app/uploads
```
