# Handoff — CineCat · Fase 3 (web públic + valoracions anònimes)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — rellevants ara: **§4** (endpoints de `ratings`, exemple de valoració anònima, codis d'error), **§5 "Cicle de vida B"** (visitant anònim valora, amb la mitigació d'abús), **§6 Frontend web** (Vue 3 + Vite, Vue Router, Pinia, `fetch`), **§7** (carpetes de `web/src/`) i **§8** (pantalles i variants d'estat).
- `docs/design/README.md` — **contracte visual**. Ara importen: sistema de disseny, **01 Catàleg** (+ 01b sense resultats, 01c càrrega), **02 Fitxa** (+ 02b sense valoracions), components **`MovieCard`** i **`RatingSelector`**, botons/xips/inputs, *Interactions & Behavior*, *State Management* i **Design Tokens**. Obre `docs/design/CineCat.dc.html` al navegador per veure-ho.
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 3.**

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-3-web-public` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix** si no t'ho demana explícitament.
- En començar: `git checkout main && git pull --prune`.
- No escriguis codi fora de l'abast d'aquest bloc (res d'auth, admin ni mòbil).
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.

## 3. Estat actual del repo

- **Branca base:** `main` a `17cdc87` (Merge PR #8). **Verifica-ho** amb `git log -1 --oneline` després del `git pull`.
- **Fet fins ara:**
  - **Fase 0** (PR #5): esquelet Go, `Dockerfile` multi-etapa, `railway.json` amb healthcheck `/health`.
  - **Fase 1** (PR #6): taules `users`/`movies`/`ratings` (migració aplicada en arrencar), CRUD `/api/movies` amb `?q=`/`?genre=`, fitxa amb `avg_score`/`rating_count` calculats per SQL, `scripts/seed.sh` (7 pel·lícules del disseny).
  - **Fase 2** (PR #8): `POST /api/movies/{id}/poster` (413/415, 500px, JPEG) i `GET /uploads/posters/<id>.jpg` (cache `no-cache` + `304`). `UPLOAD_DIR`/`MAX_UPLOAD_MB`. Go **1.26** (Dockerfile inclòs).
- **Patrons del backend a reaprofitar:** `internal/handlers/respond.go` (`writeJSON`, `writeError`, `serverError`), `movieID()` (id no-UUID → `404`), `decodeMovieInput` (JSON amb `DisallowUnknownFields` + límit de mida + `Normalize`/`Validate` a `models`), `storage` amb SQL parametritzat i `ErrNotFound`. Els nous endpoints de ratings han de seguir el mateix estil.
- **`web/`** només conté un `README.md` placeholder. Entorn local: Node **22.17**, npm 10.9.
- **⚠️ Railway NO està desplegat.** `backend` i `Postgres` estan en estat **REMOVED** des del 26/06/2026 (cal que l'usuari reactivi el compte). Les ordres pendents (Postgres, `DATABASE_URL`, volum `/app/uploads`, `UPLOAD_DIR`, deploy) són a la descripció del PR #8 i al `README.md`. Les ordres `railway` de desplegament les bloqueja el classificador de permisos: proposa-les a l'usuari, no les forcis. **Aquesta fase es fa i es verifica en local.**
- **Fase del pla on som:** Fase 3 (la quarta).

## 4. El bloc d'AQUESTA sessió — Fase 3

**Objectiu:** un catàleg navegable i valorable des del navegador, encara sense login. Qualsevol visitant pot obrir una fitxa i deixar-hi una valoració anònima que actualitza la mitjana.

**Branca a crear:** `feat/fase-3-web-public`

**Ordre recomanat:** primer el backend (endpoints de valoració, provats amb `curl`), en un commit propi; després el web. Si el context de la sessió creix massa, és acceptable tancar el backend en un PR i deixar el web per a una sessió nova amb un handoff 3b.

**Tasques — backend:**
- [ ] **`GET /api/movies/{id}/ratings?page=`**: llista paginada (mida de pàgina fixa, p. ex. 20; més recents primer). `404` si la pel·lícula no existeix.
- [ ] **`POST /api/movies/{id}/ratings`**: `{score, comment?, author_label?}` → `201` amb la valoració (format de l'exemple de §4, `user_id: null`). Sense token → `user_id = NULL`.
  - [ ] Validació: `score` enter 1–10 (`400` `{"error":"score ha d'estar entre 1 i 10"}`), longitud màxima de `comment` i `author_label`, camps desconeguts → `400`.
  - [ ] **Rate limit per IP** mínim (§5, Cicle B) → `429`. Pensa en la IP real darrere del proxy de Railway (`X-Forwarded-For`) i explica-ho.
- [ ] **`GET /api/movies`** ha de retornar també **`avg_score` i `rating_count`** per a cada pel·lícula: la `MovieCard` mostra el xip de nota. Fes-ho amb una sola consulta (`LEFT JOIN` + `GROUP BY`, o una subconsulta), no amb N consultes.
- [ ] Models a `internal/models/rating.go`; SQL a `internal/storage/ratings.go`; handlers a `internal/handlers/ratings.go`.

**Tasques — web (`web/`):**
- [ ] Inicialitzar **Vue 3 + Vite + Vue Router + Pinia** (JavaScript o TypeScript: decideix i justifica). Estructura de §7: `views/`, `components/`, `stores/`, `api/`.
- [ ] **Proxy de Vite** perquè `/api` i `/uploads` vagin a `http://localhost:8080` en desenvolupament: així el web i l'API semblen el mateix origen, `poster_url` (relativa) funciona tal qual i no cal CORS.
- [ ] Capa **`api/`**: funcions `fetch` per a cada endpoint que es fa servir; converteixen els errors `{error}` de l'API en excepcions amb el missatge.
- [ ] **Design tokens** com a variables CSS (colors, tipografia Geist/Geist Mono, radis, espaiats) segons `docs/design/README.md`. Tema fosc.
- [ ] Components reutilitzables **`MovieCard`** (pòster 2:3, xip de nota, títol, any; estat "sense pòster") i **`RatingSelector`** (1–10 amb hover i clic).
- [ ] Vista **Catàleg** (01): capçalera, cerca per títol (amb debounce), xips de gènere, graella de `MovieCard`. Variants **01b sense resultats** (amb "Esborra els filtres") i **01c càrrega** (skeletons). La cerca i el gènere, a la query de la URL (`/?q=…&genre=…`), perquè es puguin compartir i el botó enrere funcioni.
- [ ] Vista **Fitxa** (02): pòster, metadades (`2021 · 1h 52min · Dir. …`), mitjana (format català: `7,8`) i nombre de valoracions, sinopsi, llista de valoracions (paginada: "Carrega'n més") i formulari de valoració anònima. Variant **02b sense valoracions** (nota "—").
- [ ] Després d'enviar una valoració: refrescar la fitxa (mitjana i llista) i netejar el formulari. Mostrar els errors de l'API (`400`, `429`) al formulari.

**Fitxers/carpetes implicats:** `backend/internal/{models,storage,handlers}/` (ratings + llistat amb mitjana), `backend/cmd/server/main.go` (rutes, rate limit), `web/` (projecte nou sencer), `README.md` (com arrencar el web).

**FORA d'abast (no tocar ara):**
- Auth, login/registre, valoració **registrada**, `PUT /api/ratings/{id}` (Fase 4). Al formulari, el segmented control "Anònim / Com a @…" es mostra amb l'opció registrada **deshabilitada** (o amagada).
- Vistes d'admin i pujada de pòsters des del web (Fase 4).
- "+ A la meva llista" (watchlist) i la nav "Novetats / Top valorades" (fora de l'MVP, §8): es poden maquetar però no fan res.
- Servir el build de Vue des de Go i el desplegament del web (es decidirà a la Fase 6 / §9 pas 7).
- Mòbil.

## 5. Com es verifica (Definition of Done)

- [ ] `curl`: `POST .../ratings` amb `{"score":8,"comment":"…","author_label":"Joan"}` → `201` i `user_id: null`; `score: 11` → `400`; massa peticions seguides → `429`; pel·lícula inexistent → `404`.
- [ ] `GET .../ratings?page=1` i `?page=2` retornen pàgines diferents, les més recents primer.
- [ ] `GET /api/movies` inclou `avg_score`/`rating_count` per a cada pel·lícula (`null`/`0` sense valoracions).
- [ ] A la BD, la fila nova de `ratings` té `user_id = NULL`.
- [ ] Amb `npm run dev` + backend local (amb `seed.sh` i uns quants pòsters pujats): navegues pel catàleg, cerques, filtres per gènere (i la URL ho reflecteix), obres una fitxa i hi veus el pòster.
- [ ] Deixes una valoració anònima des del web i la mitjana i el recompte s'actualitzen sense recarregar la pàgina.
- [ ] Es veuen les variants: sense resultats, càrrega (skeleton) i fitxa sense valoracions; una pel·lícula sense pòster mostra el placeholder.
- [ ] El web s'assembla al disseny (tokens, tipografia, `MovieCard`, `RatingSelector`) i funciona a amplada de mòbil sense scroll horitzontal.
- [ ] `npm run build` sense errors; `go vet ./...` net i `docker build` del backend correcte.
- [ ] PR obert cap a `main` amb descripció clara, captures del web i el checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- **Anònim vs. registrat** (§3): una sola taula `ratings` amb `user_id` nullable i **el mateix endpoint** per a tots dos casos. A la Fase 4 només s'hi afegirà "si hi ha token, omple `user_id`".
- **Contracte d'API:** mana `ESPECIFICACIO.md §4` (`user_id`, `author_label`, `comment`, `created_at` en snake_case). El *Data model* de `docs/design/README.md` (camelCase, `author: {alias}`) és orientatiu; si cal, adapta'l a la capa `api/` del web, no a l'API.
- **Mitigació d'abús (§5):** rate limit per IP + validació de `score` i de longitud. **No** CAPTCHA ni moderació. Un limitador en memòria (p. ex. `golang.org/x/time/rate` per IP, o un comptador amb finestra) és suficient: hi ha una sola instància. Explica'n la limitació (es perd en reiniciar; no serveix amb N rèpliques).
- **La mitjana no es guarda** (Fase 1): tant la fitxa com el llistat la calculen amb `AVG`/`COUNT`. No afegeixis columnes de mitjana a `movies`.
- **Esborranys:** `GET /api/movies` retorna encara les pel·lícules `draft` (no hi ha auth per distingir l'admin). Decideix si el web públic les amaga (filtre al client o un paràmetre `?status=published`) i explica-ho; la separació definitiva arriba a la Fase 4.
- **Pòsters:** `poster_url` és relativa (`/uploads/posters/<id>.jpg`); amb el proxy de Vite funciona sense canvis. Proporció 2:3 amb `object-fit: cover`.
- **Fonts:** Geist i Geist Mono (Google Fonts o paquet npm `geist`).
- **Checkpoint d'aprenentatge de la fase:** entendre com el web consumeix l'API (proxy, `fetch`, estats de càrrega/error) i com una ressenya anònima viatja fins a la BD amb `user_id = NULL` pel mateix endpoint que faran servir les registrades.
