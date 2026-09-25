# CineCat

Catàleg de pel·lícules amb valoracions del públic, **web (Vue 3) + mòbil (Kotlin Multiplatform)** sobre una **API Go + PostgreSQL**, desplegat a **Railway**. Projecte d'aprenentatge: prioritzem la **claredat** sobre la completesa.

> Documentació de disseny (font de veritat): [`docs/`](docs/) — comença per [docs/README.md](docs/README.md), el [pla per fases](docs/PLA-IMPLEMENTACIO.md) i l'[especificació](docs/ESPECIFICACIO.md).

## Estructura del monorepo

```
cinecat/
├── backend/   ← API Go
├── web/       ← frontend Vue 3 (web públic)
├── mobile/    ← client KMP        (placeholder, Fase 5)
└── docs/      ← disseny i pla d'implementació
```

## Estat actual — Fase 4

- **Backend:** base de dades (`users`, `movies`, `ratings`), CRUD del catàleg a `/api/movies` (amb `avg_score` i `rating_count` calculats per SQL), pòsters (`POST /api/movies/{id}/poster` → `/uploads/posters/<id>.jpg`) i **valoracions anònimes** (`GET`/`POST /api/movies/{id}/ratings`, amb límit de peticions per IP).
- **Web públic** (`web/`): catàleg amb cerca i filtre per gènere, fitxa amb pòster, mitjana i valoracions, i formulari per valorar sense compte.
- **Auth** amb JWT: registre i login (`/api/auth/*`), tres casos (anònim / `user` / `admin`). Crear, editar, esborrar pel·lícules i pujar pòsters és només per a `admin`; els esborranys només els veu l'admin. Els usuaris registrats valoren amb el seu àlies i poden editar la seva valoració.

## Arrencar el backend en local

Variables d'entorn:

- `DATABASE_URL` (obligatòria): cadena de connexió a PostgreSQL.
- `JWT_SECRET` (obligatòria, mínim 32 caràcters): secret per signar els tokens de sessió. Genera'n un amb `openssl rand -base64 48` i no el posis mai al repo.
- `PORT` (opcional, per defecte `8080`).
- `UPLOAD_DIR` (opcional, per defecte `uploads`, relatiu a on s'executa: `backend/uploads` amb `go run`). Carpeta on es desen els pòsters; es crea sola.
- `MAX_UPLOAD_MB` (opcional, per defecte `5`). Mida màxima d'un pòster.
- `TRUST_PROXY` (opcional, per defecte fals). Posa-la a `true` només si hi ha un proxy davant (Railway): llavors la IP del visitant, per al límit de valoracions, es llegeix de `X-Forwarded-For`.

En arrencar, **aplica automàticament les migracions** pendents de `backend/migrations/`.

### 1. Una base de dades PostgreSQL

La manera més ràpida és un contenidor d'usar i llençar:

```bash
docker run -d --name cinecat-pg -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=cinecat -p 5432:5432 postgres:16-alpine
```

### 2a. El servidor amb Go instal·lat

```bash
cd backend
export DATABASE_URL='postgres://postgres:dev@localhost:5432/cinecat?sslmode=disable'
export JWT_SECRET="$(openssl rand -base64 48)"
go run ./cmd/server
```

### 2b. El servidor amb Docker (no cal tenir Go instal·lat)

Fem servir el **mateix Dockerfile** que Railway, així el que funciona en local funciona desplegat.

```bash
cd backend
docker build -t cinecat-backend .
docker run --rm -p 8080:8080 -e JWT_SECRET="$(openssl rand -base64 48)" -e DATABASE_URL='postgres://postgres:dev@host.docker.internal:5432/cinecat?sslmode=disable' cinecat-backend
```

### 3. El primer admin

Registrar-se per l'API sempre dona el rol `user`. L'admin es crea amb una comanda que parla directament amb la BD; la contrasenya es demana pel terminal sense mostrar-la (o es llegeix de `CINECAT_ADMIN_PASSWORD`), mai com a argument:

```bash
cd backend
go run ./cmd/createadmin -email admin@exemple.cat -alias admin
```

Si l'email ja és d'un usuari registrat, el promociona a admin sense tocar-ne la contrasenya.

### 4. Dades de prova i comprovació

Crear pel·lícules és només per a admins, així que el seed necessita un token d'admin:

```bash
cd backend
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@exemple.cat","password":"…"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
API_URL=http://localhost:8080 TOKEN=$TOKEN ./scripts/seed.sh   # 7 pel·lícules del disseny, via POST
curl http://localhost:8080/health                  # {"status":"ok"}
curl 'http://localhost:8080/api/movies?genre=Drama'
curl 'http://localhost:8080/api/movies?q=nits'
```

### 5. Valorar

```bash
curl -X POST http://localhost:8080/api/movies/<id>/ratings \
  -H 'Content-Type: application/json' -d '{"score": 8, "comment": "Molt bona", "author_label": "Joan"}'
curl http://localhost:8080/api/movies/<id>/ratings?page=1
```

Amb `-H "Authorization: Bearer <token>"` la valoració queda associada a l'usuari (se'n mostra l'àlies) i després es pot editar amb `PUT /api/ratings/<id>`.

### 6. Pujar un pòster (admin)

```bash
curl -H "Authorization: Bearer $TOKEN" -F "file=@poster.jpg" http://localhost:8080/api/movies/<id>/poster
# {"poster_url":"/uploads/posters/<id>.jpg"}  → obre http://localhost:8080/uploads/posters/<id>.jpg
```

Només s'accepten JPG i PNG (comprovats pel contingut, no per l'extensió → `415`) de fins a `MAX_UPLOAD_MB` (→ `413`). El servidor el redimensiona a 500px d'amplada com a màxim i el desa sempre com a JPEG.

## Arrencar el web en local

Amb el backend corrent a `:8080`:

```bash
cd web
npm install
npm run dev        # http://localhost:5173
```

Vite reenvia `/api` i `/uploads` al backend (proxy a `web/vite.config.js`), així el navegador ho veu tot com un sol origen i no cal CORS. Més detalls a [`web/README.md`](web/README.md).

## Desplegament a Railway

El backend es desplega a Railway des de la CLI amb el seu `Dockerfile`. La configuració del build i el healthcheck (`/health`) viuen a [`backend/railway.json`](backend/railway.json). El detall de les ordres usades es documenta a la descripció del PR de cada fase.

Variables d'entorn a Railway: `PORT` (la injecta Railway), `DATABASE_URL` (referenciada al plugin Postgres: `${{Postgres.DATABASE_URL}}`), `UPLOAD_DIR=/app/uploads`, `TRUST_PROXY=true` i `JWT_SECRET` (veure [ESPECIFICACIO.md §9](docs/ESPECIFICACIO.md#9-pla-de-desplegament-a-railway)).

### Volum per als pòsters

El disc d'un contenidor **s'esborra a cada desplegament**. Perquè els pòsters sobrevisquin, el servei `backend` necessita un **volum persistent** muntat a `/app/uploads` (el `WORKDIR` de la imatge és `/app`):

```bash
railway volume --service backend add --mount-path /app/uploads
railway variables --service backend --set UPLOAD_DIR=/app/uploads
```

### Primer admin a Railway

`createadmin` s'executa des de la teva màquina contra la BD de Railway. Des de fora de Railway cal la URL **pública** de Postgres (`DATABASE_PUBLIC_URL`), no la interna:

```bash
cd backend
DATABASE_URL='<DATABASE_PUBLIC_URL del servei Postgres>' go run ./cmd/createadmin -email admin@exemple.cat -alias admin
```
