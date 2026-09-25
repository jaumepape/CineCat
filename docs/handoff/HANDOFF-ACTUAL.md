# Handoff — CineCat · Fase 6 (desplegament a Railway i proves de punta a punta)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — rellevants ara: **§5** (arquitectura: una API, dos clients, el volum), **§9** (pla de desplegament a Railway: serveis, "Frontend Vue — Opció A: servir el build des de l'API Go", volum a `/app/uploads`, variables, verificació de punta a punta).
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 6, l'última.**
- `README.md` arrel (seccions de Railway, primer admin, variables), `mobile/README.md` i `web/README.md`.

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-6-deploy` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix** si no t'ho demana explícitament.
- En començar: `git checkout main && git pull --prune`.
- **Cap acció a producció sense confirmació explícita de l'usuari** (deploy, variables, volums, BD). A més, el classificador de permisos bloqueja les ordres `railway` que despleguen: **proposa-les a l'usuari** perquè les executi o hi doni permís; no intentis esquivar el bloqueig.
- **Mai secrets al repo ni al xat:** `JWT_SECRET` es genera i es posa directament a Railway; la contrasenya de l'admin la tecleja l'usuari.
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.

## 3. Estat actual del repo

- **Branca base:** `main` a `565de1f` (Merge PR #14). **Verifica-ho** amb `git log -1 --oneline` després del `git pull`.
- **Fet fins ara (tot provat en local):**
  - **Backend** (Fases 0–4, `backend/`): API Go + PostgreSQL. Migracions aplicades en arrencar. Catàleg, pòsters (`UPLOAD_DIR`, `MAX_UPLOAD_MB`), valoracions anònimes/registrades, auth JWT, rols, límit per IP amb `TRUST_PROXY`. `cmd/createadmin`. `scripts/seed.sh` (necessita `TOKEN` d'admin). `Dockerfile` a `backend/` (Go 1.26), `backend/railway.json` (builder DOCKERFILE, healthcheck `/health`).
  - **Web** (Fases 3–4, `web/`): Vue 3 + Vite. En desenvolupament, el proxy de Vite envia `/api` i `/uploads` a `:8080`. **Encara no hi ha cap manera de servir-lo en producció.**
  - **Mòbil** (Fase 5, `mobile/`): KMP + Compose Multiplatform. La URL de l'API és un `expect val defaultBaseUrl` fix per plataforma (`http://10.0.2.2:8080` / `http://localhost:8080`, a `shared/src/{androidMain,iosMain}/.../data/Platform.*.kt`). `AppContainer(tokenStorage, baseUrl)` ja accepta una URL.
  - **Límit d'abús** per a les valoracions anònimes: **ja fet a la Fase 3** (el pla el posa a la Fase 6); aquí només cal verificar-lo darrere del proxy de Railway.
- **⚠️ Railway, estat real** (projecte `CineCat`, entorn `production`, compte de l'usuari):
  - Serveis `backend` i `Postgres` en estat **Offline / REMOVED** des del 26/06/2026 (probablement el període de prova acabat). El volum `postgres-volume` existeix. **L'usuari ha de reactivar el compte/pla abans de res.**
  - Encara **no** hi ha: `DATABASE_URL` referenciada al backend, `JWT_SECRET`, `TRUST_PROXY`, `UPLOAD_DIR`, ni el volum de pòsters.
  - Railway CLI instal·lada: v5.23.3 (n'hi ha una de més nova, v5.62.1: suggereix `railway upgrade`). El projecte està enllaçat al directori arrel del repo.
- **Proves manuals pendents de fases anteriors** (a fer durant la prova final): formularis de login/registre (web i mòbil), valoració amb `@àlies` des del mòbil, i la fitxa i el full de valorar tocant a iOS.
- **Eines locals:** Go 1.26, Node 22.17, Docker, JDK = JBR d'Android Studio (`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`), emulador Android `mundial`, Xcode 26.5 + simulador iPhone 17 Pro. El panell del simulador de l'app de Claude demanava `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` (ho ha de fer l'usuari).
- **Fase del pla on som:** Fase 6 (la setena i última).

## 4. El bloc d'AQUESTA sessió — Fase 6

**Objectiu:** tot en producció a Railway (API + web + BD + pòsters en un volum) i verificat de punta a punta amb el web i el mòbil contra la URL pública HTTPS.

**Branca a crear:** `feat/fase-6-deploy`

**Ordre recomanat:** (1) preparar el codi i provar-lo en local amb Docker → (2) PR amb els canvis de codi → (3) amb l'usuari, reactivar i configurar Railway i desplegar → (4) prova de punta a punta → (5) documentar el resultat. Els passos 3–4 depenen de l'usuari: si Railway no està a punt, tanca el PR del codi (1–2) i deixa escrit exactament què falta.

### Codi

- [ ] **Servir el web des de l'API Go** (§9, Opció A: una sola URL, sense CORS). La imatge ha de construir el web:
  - Dockerfile **multi-etapa amb tres etapes**: Node (`npm ci && npm run build` a `web/`) → Go (build) → runtime amb el binari + `web/dist`. Com que ara el build necessita `web/` i `backend/`, el **context de build ha de ser l'arrel del repo**: decideix on viu el Dockerfile i ajusta `railway.json` (i com es desplega: `railway up` des de l'arrel).
  - A Go: servir fitxers estàtics des d'un `WEB_DIR` (p. ex. `/app/web`), amb **fallback a `index.html`** per a les rutes del SPA (`/pelicula/…`, `/admin/…`, `/entra`) però **mai** per a `/api/*`, `/uploads/*` ni `/health` (un endpoint d'API inexistent ha de continuar donant un `404` en JSON).
  - Cache: `index.html` sense cache (`no-cache`); els fitxers amb hash de `web/dist/assets/` amb cache llarga (`immutable`). Explica per què.
  - Si `WEB_DIR` no existeix (desenvolupament amb Vite), l'API funciona igual que ara.
- [ ] **URL de l'API al mòbil per entorn:** debug → local (com ara); release → la URL HTTPS de Railway. Recomanació: que cada app la passi a `AppContainer(baseUrl = …)` des de la seva configuració (Android: `buildConfigField` per build type; iOS: una clau a `Info.plist` alimentada per l'`xcconfig` de cada configuració), i que `defaultBaseUrl` quedi només com a valor de desenvolupament. En release no cal cap excepció d'HTTP en clar (ja són només de debug).
- [ ] **Documentació:** `README.md` (com desplegar ara: una sola imatge amb API + web), `ESPECIFICACIO.md` §9 si alguna decisió canvia, i `mobile/README.md` (com apuntar a producció).

### Railway (amb l'usuari, pas a pas i amb confirmació)

- [ ] Reactivar el compte/pla i tornar a aixecar `Postgres`.
- [ ] Al servei `backend`: `DATABASE_URL=${{Postgres.DATABASE_URL}}`, `JWT_SECRET` (generat amb `openssl rand -base64 48`, posat directament a Railway), `UPLOAD_DIR=/app/uploads`, `TRUST_PROXY=true`, i `WEB_DIR` si cal.
- [ ] **Volum** muntat a `/app/uploads` al servei `backend`.
- [ ] Desplegar i comprovar `GET /health`, la web a `/` i `GET /api/movies`.
- [ ] **Primer admin** amb `createadmin` des de la màquina de l'usuari contra `DATABASE_PUBLIC_URL` (vegeu el README); la contrasenya la tecleja l'usuari.
- [ ] **Verificar la IP real darrere del proxy:** que el límit per IP de Railway limita per visitant i no globalment (per exemple, enviant 6 valoracions seguides des d'una xarxa i comprovant que una altra xarxa no queda bloquejada; o registrant temporalment, sense dades personals, quina entrada de `X-Forwarded-For` es fa servir). Si Railway posa la IP en una altra posició, ajusta `clientIP` i explica-ho.
- [ ] Decidir amb l'usuari si es carreguen dades de demostració a producció (`seed.sh` amb el token d'admin) o si es comença buit.

## 5. Com es verifica (Definition of Done)

Recorregut de punta a punta del pla (§9), **contra la URL pública HTTPS**:

- [ ] 1. **Admin web** → inicia sessió → crea una pel·lícula → hi puja el pòster.
- [ ] 2. El pòster es veu a la **fitxa web**.
- [ ] 3. **Mòbil** (build release apuntant a producció, Android i iOS) → el mateix pòster es carrega des de la mateixa URL.
- [ ] 4. **Redeploy** de l'API → **el pòster segueix allà** (volum persistent) i la sessió de l'admin continua valent (mateix `JWT_SECRET`).
- [ ] 5. **Visitant anònim** valora (web i mòbil) → la mitjana s'actualitza a tots dos clients.
- [ ] Rutes del SPA en recarregar la pàgina (`/pelicula/<id>`, `/admin`) → carreguen el web, no un 404; `/api/no-existeix` → `404` JSON.
- [ ] Límit per IP verificat darrere del proxy (vegeu §4).
- [ ] Proves manuals pendents fetes: login/registre (web i mòbil), valoració `@àlies` des del mòbil, fitxa i valorar a iOS.
- [ ] `docker build` des de l'arrel correcte; `go vet ./...` net; `npm run build` correcte; `./gradlew :shared:allTests` correcte.
- [ ] PR obert cap a `main` amb la descripció, la URL pública, el resultat del recorregut i el checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- **§9 ja decideix**: web servit per l'API Go (Opció A) i volum persistent a `/app/uploads` servit per Go. No introdueixis S3, CDN ni un servei estàtic separat sense preguntar.
- **Una sola instància:** el límit per IP i el volum són locals al procés/contenidor. No configuris més d'una rèplica a Railway (explica per què si surt el tema).
- **Migracions en arrencar:** ja passa; amb una sola instància no hi ha curses entre rèpliques.
- **`JWT_SECRET` estable:** si canvia en un redeploy, totes les sessions deixen de valer (els clients ja ho gestionen tancant la sessió amb el `401`, però millor que no passi).
- **Imatges:** a producció, `poster_url` continua sent relativa (`/uploads/posters/<id>.jpg`): el web la fa servir tal qual (mateix origen) i el mòbil hi afegeix la URL base (`CineCatApi.absoluteUrl`).
- **Fora d'abast:** domini propi, CI/CD automàtic, còpies de seguretat programades de la BD, monitoratge, publicació a les botigues d'apps, signatura de release per a dispositius reals (per al mòbil n'hi ha prou amb un build release a l'emulador/simulador).
- **Checkpoint d'aprenentatge de la fase:** entendre com persisteixen les imatges entre desplegaments (contenidor efímer vs. volum), com una sola imatge Docker serveix API i web des del mateix origen, i saber fer una prova completa de punta a punta amb tots dos clients.
