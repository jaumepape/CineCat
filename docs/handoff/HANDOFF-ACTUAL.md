# Handoff — CineCat · Fase 2 (pujada i servei d'imatges: pòsters)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — rellevants ara: **§4** (endpoints `/poster` i `/uploads/...`, "Pujada d'imatge (conceptual)", codis `413`/`415`), **§5** (el flux d'una imatge de punta a punta, passos a–f), **§6** (`disintegration/imaging`, `mime/multipart`) i **§9** ("On viuen les imatges en producció", variables `UPLOAD_DIR` i `MAX_UPLOAD_MB`).
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 2.**

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-2-posters` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix** si no t'ho demana explícitament.
- En començar: `git checkout main && git pull --prune`.
- No escriguis codi fora de l'abast d'aquest bloc (res d'auth, valoracions, frontend ni mòbil).
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.

## 3. Estat actual del repo

- **Branca base:** `main` a `e5f56d7` (Merge PR #6). **Verifica-ho** amb `git log -1 --oneline` després del `git pull`.
- **Fet fins ara:**
  - **Fase 0** (PR #5): esquelet Go, `Dockerfile` multi-etapa, `railway.json` amb healthcheck `/health`.
  - **Fase 1** (PR #6): migració `000001_init` (taules `users`, `movies`, `ratings`), pool `pgx` des de `DATABASE_URL`, migracions **aplicades en arrencar** (SQL incrustat amb `go:embed`), router `chi`, CRUD `/api/movies` amb `avg_score`/`rating_count` calculats per SQL, validació `400`/`404` i `backend/scripts/seed.sh` (7 pel·lícules via `POST`). Go **1.25**.
- **Codi que ja prepara aquesta fase:**
  - `movies.poster_path` existeix (nullable) i està sempre a `NULL`.
  - `internal/storage/movies.go` ja construeix `poster_url` com a `'/uploads/' || poster_path` → si guardes `poster_path = 'posters/<id>.jpg'`, l'API retornarà sola `"/uploads/posters/<id>.jpg"`. No cal tocar els `SELECT`.
  - Els handlers segueixen el patró `respond.go` (`writeJSON`, `writeError`, `serverError`) i `movieID()` (id no-UUID → `404`). Reaprofita'ls.
  - `.gitignore` ja ignora `/backend/uploads/`.
- **⚠️ Railway NO està desplegat.** Els serveis `backend` i `Postgres` estan en estat **REMOVED** des del 26/06/2026 (probablement va acabar el període de prova). El volum `postgres-volume` existeix. L'usuari ha de revisar el pla del compte i reactivar-ho; `DATABASE_URL = ${{Postgres.DATABASE_URL}}` encara **no** s'ha afegit al servei `backend`. Les ordres `railway` de desplegament les bloqueja el classificador de permisos: proposa-les a l'usuari perquè les executi (o que et doni permís), no les forcis.
- **Fase del pla on som:** Fase 2 (la tercera).

## 4. El bloc d'AQUESTA sessió — Fase 2

**Objectiu:** el cicle de vida complet d'una imatge: pujar un pòster, validar-lo, redimensionar-lo, desar els bytes al disc, desar la ruta a la BD i servir-lo per URL. És la fase conceptualment més important del projecte.

**Branca a crear:** `feat/fase-2-posters`

**Tasques concretes:**
- [ ] **Configuració:** llegir `UPLOAD_DIR` (per defecte `uploads`, relatiu a on corre el binari; en local `backend/uploads`) i `MAX_UPLOAD_MB` (per defecte `5`). Crear `UPLOAD_DIR/posters/` a l'arrencada si no existeix.
- [ ] **`POST /api/movies/{id}/poster`** (`multipart/form-data`, camp `file`):
  - [ ] La pel·lícula ha d'existir → `404` si no.
  - [ ] **Mida** ≤ `MAX_UPLOAD_MB` → `413` `{"error":"màxim 5 MB"}`. Limita la lectura (`http.MaxBytesReader`) **abans** de parsejar, no després.
  - [ ] **Tipus real pels magic bytes** (`http.DetectContentType` amb els primers 512 bytes, o comprovació manual): només JPEG/PNG → `415` `{"error":"només JPG o PNG"}`. Ignora l'extensió i el `Content-Type` que diu el client.
  - [ ] **Redimensionar** a amplada màxima ~500px mantenint la proporció (`disintegration/imaging`). No ampliïs imatges petites.
  - [ ] **Nom segur** generat pel servidor: `posters/<id>.jpg`. Mai el nom que envia el client.
  - [ ] **Desar al disc** i fer `UPDATE movies SET poster_path = ...` (nou mètode a `storage`).
  - [ ] Retornar `200` amb `{"poster_url": "/uploads/posters/<id>.jpg"}`.
- [ ] **`GET /uploads/posters/{file}`**: servir els fitxers estàtics de `UPLOAD_DIR` (p. ex. `http.FileServer` + `http.StripPrefix`). Sense llistat de directoris. Cal que sigui impossible sortir de `UPLOAD_DIR` (`../`).
- [ ] `GET /api/movies` i `GET /api/movies/{id}` retornen `poster_url` quan hi ha pòster (ja hauria de funcionar sol; verifica-ho).
- [ ] **Railway:** documentar (i, si l'usuari ho aprova, fer) el **volum persistent** muntat a `/app/uploads` i `UPLOAD_DIR=/app/uploads` al servei `backend`.

**Fitxers/carpetes implicats:** `backend/internal/handlers/` (nou `posters.go`), `backend/internal/storage/` (desar fitxer + `UPDATE poster_path`), `backend/cmd/server/main.go` (config + rutes), `backend/go.mod` (`disintegration/imaging`), `README.md` (variables i prova amb `curl -F`).

**FORA d'abast (no tocar ara):**
- Auth/JWT i protecció de l'endpoint de pujada (Fase 4): de moment queda **obert**, com el CRUD.
- Valoracions (Fase 3), frontend web, mòbil.
- CDN, emmagatzematge d'objectes (S3/R2), miniatures múltiples, processament asíncron (descartats a §1).

## 5. Com es verifica (Definition of Done)

- [ ] `curl -F "file=@poster.jpg" http://localhost:8080/api/movies/<id>/poster` → `200` + `{"poster_url": ...}`.
- [ ] Obrir `http://localhost:8080<poster_url>` al navegador mostra la imatge **redimensionada** (amplada ≤ 500px) i `Content-Type: image/jpeg`.
- [ ] Un PNG també s'accepta. Un fitxer > 5 MB dona `413`. Un `.txt` reanomenat a `.jpg` dona `415`. Un id inexistent dona `404`.
- [ ] Pujar un segon pòster a la mateixa pel·lícula el **substitueix** (el mateix fitxer, sense fitxers orfes).
- [ ] `GET /api/movies/<id>` retorna el `poster_url` nou; les pel·lícules sense pòster continuen amb `null`.
- [ ] `GET /uploads/posters/../../etc/passwd` (i variants) **no** surt de `UPLOAD_DIR`.
- [ ] `GET /health` i el CRUD de la Fase 1 continuen funcionant; `go vet ./...` net i `docker build` correcte.
- [ ] PR obert cap a `main` amb descripció clara i el checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- **La BD guarda la ruta, el disc els bytes** (§3). Mai bytes a PostgreSQL.
- **Volum persistent (§9):** el disc d'un contenidor és efímer; sense volum, cada redeploy esborra els pòsters. El codi és idèntic en dev i prod: només canvia que en prod `UPLOAD_DIR` és un volum. Muntatge a `/app/uploads` (el `WORKDIR` del `Dockerfile` és `/app`).
- **Decisions que has de prendre i explicar:**
  - **Recodificar sempre a JPEG:** encaixa amb el nom `<id>.jpg` i el `image/jpeg` de §4, i en recodificar s'eliminen les metadades EXIF (GPS, etc.) i qualsevol contingut amagat al fitxer original. Contrapartida: un PNG amb transparència perd el canal alfa (compon-lo sobre un fons fosc o blanc).
  - **Cache en substituir:** si el nom és sempre `<id>.jpg`, el navegador pot mostrar el pòster vell. Opcions: capçalera `Cache-Control` curta, o un paràmetre de versió a la URL. Tria'n una i justifica-la.
  - **Esborrar una pel·lícula:** el `DELETE` de la Fase 1 no esborra el fitxer del pòster. Decideix si ara ho fa (recomanat: sí, i sense fallar si el fitxer no hi és).
  - **Escriptura atòmica:** escriure a un fitxer temporal i fer `os.Rename` evita servir mig pòster si algú el demana mentre es desa.
- **Límit de mida:** `MAX_UPLOAD_MB` limita el cos de la petició; tingues en compte que el multipart afegeix uns pocs bytes de capçaleres respecte al fitxer.
- **Checkpoint d'aprenentatge de la fase:** entendre que la BD guarda la ruta i el disc els bytes (i per què); per què es valida mida i tipus, i per què pels magic bytes i no per l'extensió; què és un volum persistent i per què cal.
