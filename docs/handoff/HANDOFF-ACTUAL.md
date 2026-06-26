# Handoff — CineCat · Fase 1 (BD + API de catàleg, sense imatges)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — **§3 (model de dades)**, **§4 (API REST)** i **§6 (stack)** són els rellevants ara. §3 té les 3 taules columna a columna; §4 té les rutes, els cossos JSON d'exemple i els codis d'error.
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 1.**

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-1-catalog` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix.**
- En començar: `git checkout main && git pull --prune`. El remote esborra la branca automàticament en fer merge.
- No escriguis codi fora de l'abast d'aquest bloc (res d'imatges, auth, frontend ni mòbil).
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.

## 3. Estat actual del repo

- **Branca base:** `main` un cop fusionat el PR de la Fase 0. **Verifica-ho** amb `git log -1 --oneline` després del `git pull`.
- **Fet fins ara:**
  - Documentació (disseny, pla, plantilla de handoff).
  - **Fase 0 completa** (PR #5): esquelet Go a `backend/` amb `GET /health → 200`, `Dockerfile` multi-etapa, `railway.json` (builder DOCKERFILE + healthcheck `/health`), placeholders `web/` i `mobile/`, README arrel.
  - **Desplegat a Railway**: projecte `CineCat` (compte `jaume@webapify.net`), entorn `production`, servei `backend` viu a `https://backend-production-e587.up.railway.app/health`, i servei **Postgres** ja afegit i online (encara **sense usar**).
- **Fase del pla on som:** Fase 1 (la segona). Existeix `backend/` amb només `cmd/server/main.go`; falten `internal/` i `migrations/`.

## 4. El bloc d'AQUESTA sessió — Fase 1

**Objectiu:** el CRUD de pel·lícules i la lectura del catàleg, tot per API contra PostgreSQL. Encara **sense imatges ni usuaris/auth**. En acabar, has de poder crear/llistar/consultar/editar/esborrar pel·lícules amb `curl`.

**Branca a crear:** `feat/fase-1-catalog`

**Tasques concretes:**
- [ ] **Migració inicial** amb les **3 taules** (`users`, `movies`, `ratings`) segons `ESPECIFICACIO.md §3`. `movies.poster_path` i `ratings.user_id` són **nullable**. Usa `golang-migrate` (SQL versionat a `backend/migrations/`).
- [ ] **Connexió a PostgreSQL** llegint `DATABASE_URL` amb `pgx` (pool a l'arrencada). Mai credencials al codi.
- [ ] Introduir el router **`chi`** (decidit a §6) i moure `/health` cap a ell. Estructura segons §7: `internal/handlers/`, `internal/models/`, `internal/storage/`.
- [ ] **Endpoints de catàleg** (de moment SENSE protegir; l'auth és la Fase 4):
  - [ ] `GET /api/movies` amb filtres `?q=` (títol) i `?genre=` (`WHERE $1 = ANY(genres)`).
  - [ ] `GET /api/movies/{id}` — fitxa + `avg_score` (`AVG`) + `rating_count` (`COUNT`) calculats amb SQL, no guardats.
  - [ ] `POST /api/movies`, `PUT /api/movies/{id}`, `DELETE /api/movies/{id}` (→ `204`).
- [ ] **Validació** bàsica i codis d'error `400` (cos invàlid) i `404` (inexistent), tal com defineix §4.
- [ ] **Dades de prova**: carregar unes quantes pel·lícules (sense pòster) — seed SQL o uns quants `POST`.

**Fitxers/carpetes implicats:** `backend/migrations/` (nou), `backend/internal/{handlers,models,storage}/` (nou), `backend/cmd/server/main.go` (afegir router + pool), `backend/go.mod` (deps noves).

**FORA d'abast (no tocar ara):**
- Imatges/pòsters i l'endpoint `/poster` (Fase 2).
- Auth, JWT, rols, protecció d'endpoints (Fase 4) — els endpoints de catàleg queden oberts de moment.
- Endpoints de `ratings` per escriure (la valoració arriba a la Fase 3); aquí només es **llegeixen** per calcular la mitjana.
- Frontend i mòbil.

## 5. Com es verifica (Definition of Done)

- [ ] La migració crea les 3 taules a la BD (comprovable amb `psql`/TablePlus o `railway connect`).
- [ ] El backend obre el pool amb `DATABASE_URL` a l'arrencada i no peta si la BD és accessible.
- [ ] Amb `curl`/Postman: crear, llistar (amb `?q=` i `?genre=`), consultar, editar i esborrar una pel·lícula funciona amb els codis correctes.
- [ ] `GET /api/movies/{id}` retorna `avg_score: null` (o `0`) i `rating_count: 0` quan no hi ha valoracions.
- [ ] `GET /health` segueix viu.
- [ ] PR obert cap a `main` amb descripció clara i el checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- **Stack ja decidit (§6):** router `chi`, accés a BD amb `pgx` (comença amb SQL pla per entendre les consultes), migracions amb `golang-migrate`.
- **`genres` és `text[]`** (decisió presa a §3), validat contra una llista fixa al backend. Filtre: `WHERE $1 = ANY(genres)`. No facis taula de gèneres.
- **La mitjana no es guarda**: es calcula amb `AVG`/`COUNT` dins de `GET /api/movies/{id}`. Guardar-la duplicada seria un antipatró (raonat a §2/§3).
- **`DATABASE_URL` a Railway:** el plugin Postgres ja existeix al projecte. Cal exposar la seva URL al servei `backend` com a variable referenciada (a Railway: `${{Postgres.DATABASE_URL}}`) i, en local, via `.env`/variable d'entorn (mai al repo; `.env` ja és a `.gitignore`). El desplegament i les migracions es continuen fent per la **Railway CLI** (`railway` v5.23.1, compte `jaume@webapify.net`; ja autenticat). Un deploy a producció requereix confirmació explícita de l'usuari.
- **Migracions en arrencar vs. pas a part:** decideix i explica-ho (executar-les al boot de l'app és simple per aprendre; un pas separat és més net per a prod). Qualsevol de les dues és acceptable a la Fase 1.
- **Checkpoint d'aprenentatge de la fase:** entendre el flux **petició → handler → SQL → JSON** i per què la mitjana es calcula i no es duplica.
