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

## Estat actual — Fase 0

Tenim l'esquelet del backend amb un únic endpoint de salut, `GET /health`, llest per desplegar. Encara **no hi ha base de dades, catàleg, imatges ni auth** (arriben a les fases següents).

## Arrencar el backend en local

El servidor llegeix el port de la variable d'entorn `PORT` (per defecte `8080`).

### Opció A — amb Go instal·lat

```bash
cd backend
PORT=8080 go run ./cmd/server
```

### Opció B — amb Docker (no cal tenir Go instal·lat)

Fem servir el **mateix Dockerfile** que Railway, així el que funciona en local funciona desplegat.

```bash
cd backend
docker build -t cinecat-backend .
docker run --rm -p 8080:8080 -e PORT=8080 cinecat-backend
```

### Comprovar que respon

```bash
curl -i http://localhost:8080/health
# HTTP/1.1 200 OK
# {"status":"ok"}
```

## Desplegament a Railway

El backend es desplega a Railway des de la CLI amb el seu `Dockerfile`. La configuració del build i el healthcheck (`/health`) viuen a [`backend/railway.json`](backend/railway.json). El detall de les ordres usades es documenta a la descripció del PR de cada fase.

Variables d'entorn rellevants per fase: a la **Fase 0** només cal `PORT` (que Railway injecta). Més endavant: `DATABASE_URL`, `JWT_SECRET`, `UPLOAD_DIR`, `MAX_UPLOAD_MB` (veure [ESPECIFICACIO.md §9](docs/ESPECIFICACIO.md#9-pla-de-desplegament-a-railway)).
