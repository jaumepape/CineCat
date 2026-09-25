# Handoff — CineCat · Fase 4 (autenticació + àrea d'administració)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — rellevants ara: **§2 Flux A** (històries de l'admin), **§3** (`users`: `alias`, `password_hash`, `role`), **§4** (auth, rutes protegides, valoració registrada, codis `401`/`403`), **§5 Cicle de vida A** (l'admin crea una pel·lícula i hi puja el pòster), **§6** (`golang-jwt/jwt`, `bcrypt`, Pinia per al token) i **§9** (`JWT_SECRET`, pas 8: crear el primer admin).
- `docs/design/README.md` — pantalles **03 Inici de sessió / Registre**, **04 Admin — llistat**, **05 Admin — formulari** (+ **05b error de pujada**), el *segmented control* Anònim / "Com a @àlies" de la **02 Fitxa**, i *State Management* (`currentUser`, `uploadError`). Obre `docs/design/CineCat.dc.html` al navegador.
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 4.**

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-4-auth-admin` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix** si no t'ho demana explícitament.
- En començar: `git checkout main && git pull --prune`.
- No escriguis codi fora de l'abast d'aquest bloc (res de mòbil ni de desplegament del web).
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.
- **Mai credencials al repo:** ni `JWT_SECRET`, ni contrasenyes de l'admin, ni hashes de prova.

## 3. Estat actual del repo

- **Branca base:** `main` a `4069118` (Merge PR #10). **Verifica-ho** amb `git log -1 --oneline` després del `git pull`.
- **Fet fins ara:**
  - **Fases 0–2** (PR #5, #6, #8): API Go amb `chi` + `pgx`, migració `000001_init` (les 3 taules; `users` existeix però **està buida i sense cap codi que la faci servir**), CRUD `/api/movies`, pòsters (`POST .../poster` + `GET /uploads/posters/<id>.jpg`).
  - **Fase 3** (PR #10): `GET`/`POST /api/movies/{id}/ratings` (anònimes, `user_id = NULL`, 20 per pàgina, límit per IP → `429`); `GET /api/movies` amb `avg_score`/`rating_count` i `?status=`; **web públic** a `web/` (Vue 3 + Vite + Router + Pinia, en **JavaScript**): catàleg, fitxa, formulari de valoració anònima.
- **Punts d'enganxament que ja esperen aquesta fase:**
  - `internal/handlers/ratings.go` → `Create` té `var userID *string // anònim; la Fase 4 l'omplirà a partir del token`.
  - `internal/handlers/ratelimit.go` → `RateLimiter` reutilitzable (p. ex. per a `/api/auth/login`).
  - `web/src/api/client.js` → `request()` és l'únic punt que fa `fetch`: és on s'ha d'afegir la capçalera `Authorization`.
  - `web/src/api/ratings.js` → `createRating` ja diu que la Fase 4 hi afegirà el token.
  - `web/src/components/RatingForm.vue` → l'opció "Com a @àlies" està **desactivada**. `AppHeader.vue` → "Inicia sessió" està **desactivat**.
  - `web/src/api/movies.js` → el web públic demana `?status=published`. **No és seguretat**: ara ho ha de fer l'API.
- **Entorn local:** Go 1.26, Node 22.17. `.claude/launch.json` (sense commit) té una configuració `web` per arrencar Vite al navegador de l'app. Per provar: Postgres amb Docker + `backend/scripts/seed.sh` (veure `README.md`).
- **⚠️ Railway NO està desplegat.** `backend` i `Postgres` estan en estat **REMOVED** des del 26/06/2026. Cal que l'usuari reactivi el compte; les ordres pendents són al PR #8 i al `README.md`, més `TRUST_PROXY=true` i, ara, `JWT_SECRET`. Les ordres `railway` de desplegament les bloqueja el classificador de permisos: proposa-les, no les forcis. **Aquesta fase es fa i es verifica en local.**
- **Fase del pla on som:** Fase 4 (la cinquena).

## 4. El bloc d'AQUESTA sessió — Fase 4

**Objectiu:** distingir els tres casos (anònim / `user` / `admin`) amb un token JWT, protegir per rol el manteniment del catàleg, permetre valoracions registrades i donar a l'admin una àrea web per crear i editar pel·lícules amb el seu pòster.

**Branca a crear:** `feat/fase-4-auth-admin`

**Ordre recomanat:** primer tot el **backend** (provat amb `curl`), en commits propis; després el **web**. És la fase més gran del projecte: si el context de la sessió creix massa, tanca el backend en un PR i genera un handoff **4b** per al web.

### Backend

- [ ] **Paquet `internal/auth/`** (§7): hash i comprovació de contrasenyes amb **bcrypt**; emissió i verificació de **JWT** (HS256 amb `JWT_SECRET`) amb `sub` (id de l'usuari), `role`, `alias` i caducitat (`exp`).
- [ ] **`JWT_SECRET` obligatori** a l'arrencada (com `DATABASE_URL`): sense secret, o si és massa curt (p. ex. < 32 bytes), el servidor no arrenca.
- [ ] **`POST /api/auth/register`** `{alias, email, password}` → `201 {token, user}`. `user` = `{id, alias, email, role}` (mai el hash). Valida el format d'email (normalitzat a minúscules), la contrasenya (mínim 8 caràcters, com el disseny) i l'àlies. Email o àlies repetit → **`409`**. Sempre es crea amb `role = 'user'`: el rol mai el tria el client.
- [ ] **`POST /api/auth/login`** `{email, password}` → `200 {token, user}`. Credencials incorrectes → `401` amb un **missatge genèric** (no diguis si l'email existeix). Aplica-hi el `RateLimiter` (força bruta).
- [ ] **Middleware d'auth** que llegeix `Authorization: Bearer <token>` i posa l'usuari al `context` de la petició: **sense token → anònim (vàlid)**; token invàlid o caducat → `401`. Més un middleware **`RequireRole("admin")`**: anònim → `401` `{"error":"no autenticat"}`, `user` → `403` `{"error":"cal rol admin"}`.
- [ ] **Protegir** `POST/PUT/DELETE /api/movies` i `POST /api/movies/{id}/poster` → només `admin`.
- [ ] **Esborranys a l'API:** `GET /api/movies` i `GET /api/movies/{id}` només retornen `draft` a l'admin; per a la resta, un esborrany és un `404` a la fitxa i no surt al llistat, encara que es demani `?status=draft`.
- [ ] **Valoració registrada:** a `POST /api/movies/{id}/ratings`, si hi ha un usuari al context, `user_id` s'omple (i s'ignora `author_label`).
- [ ] **Àlies a les valoracions:** `GET .../ratings` i les respostes de valoració inclouen l'àlies de l'autor si és registrada (`LEFT JOIN users`), mai l'email. Tria el nom del camp (p. ex. `user_alias`, `null` si és anònima) i **actualitza §4** d'`ESPECIFICACIO.md`.
- [ ] **`PUT /api/ratings/{id}`** `{score, comment?}`: només l'autor. Anònim → `401`; un altre usuari → `403`; una valoració anònima no la pot editar ningú (`403`); inexistent → `404`.
- [ ] **Primer admin:** una comanda `backend/cmd/createadmin` (p. ex. `go run ./cmd/createadmin -email … -alias …`) que llegeix la contrasenya **per stdin o per una variable d'entorn**, mai com a argument, i crea l'usuari amb `role = 'admin'` (o promociona un usuari existent). Documenta-ho al README.
- [ ] **Actualitza `ESPECIFICACIO.md` §4** amb els canvis de contracte: `register` demana també `alias`, el camp d'àlies a les valoracions i el `409`.

### Web (`web/`)

- [ ] **Store d'auth (Pinia):** `token` i `user` (`currentUser`), `login`, `register`, `logout`. Persistència del token: tria i justifica (`localStorage` és el més simple; explica el risc de XSS i per què aquí és acceptable). En arrencar, si el token ha caducat, es descarta.
- [ ] **`api/client.js`:** afegeix `Authorization: Bearer <token>` quan hi ha sessió. Un `401` en una petició autenticada tanca la sessió (token caducat o invàlid).
- [ ] **Vista 03 — Inicia sessió / Registre:** les dues targetes del disseny, el bàner "Iniciar sessió és opcional…" i "o bé continua com a visitant →". Errors de l'API (`401`, `409`, `400`) als formularis. "Has oblidat la contrasenya?" es mostra però **no** fa res (fora d'abast).
- [ ] **Capçalera:** sense sessió → "Inicia sessió"; amb sessió → xip amb l'àlies + "Surt"; si és admin → badge **ADMIN** i enllaç a l'àrea d'admin.
- [ ] **Guard de rutes:** `/admin/**` només per a `admin` (`meta.requiresAdmin`); altrament, redirigeix a login (o al catàleg si ja és `user`). **Recorda:** el guard és comoditat d'interfície; la seguretat real és el `401`/`403` de l'API.
- [ ] **Vista 04 — Admin, llistat:** la taula del disseny (miniatura, títol + vots, any, direcció, gèneres, nota, estat Publicada/Esborrany, "Edita"), cerca, "+ Afegeix pel·lícula" i esborrar amb confirmació. Hi surten també els **esborranys**.
- [ ] **Vista 05 — Admin, formulari (crear/editar):** títol, any, durada, direcció, gèneres (xips amb "×" + afegir, de la llista tancada), sinopsi i estat (publicada/esborrany); pòster amb **previsualització local** (`URL.createObjectURL`) abans de pujar-lo. Desar = `POST`/`PUT` de les metadades **i després**, si hi ha fitxer nou, `POST .../poster` (dues peticions, §4).
- [ ] **Variant 05b:** si el fitxer no és JPG/PNG o passa de 5 MB, s'avisa **al client abans de pujar-lo** (zona vermella, xip del fitxer rebutjat, "Desa" desactivat). Si igualment l'API respon `413`/`415`, es mostra el mateix estat.
- [ ] **Valoració registrada a la fitxa:** amb sessió, "Com a @àlies" s'activa i s'envia amb token; la llista mostra l'àlies. A les valoracions pròpies, un botó per **editar-les** (`PUT /api/ratings/{id}`).
- [ ] **Treure `status: 'published'`** de `api/movies.js` (ara filtra l'API) o deixar-ho explícit; explica-ho.

**Fitxers/carpetes implicats:** `backend/internal/auth/` (nou), `backend/internal/{models,storage,handlers}/` (users, auth, ratings), `backend/cmd/server/main.go`, `backend/cmd/createadmin/` (nou), possiblement `backend/migrations/000002_*.sql`, `web/src/{stores,api,views,components}/`, `web/src/router.js`, `README.md`, `docs/ESPECIFICACIO.md` (§4).

**FORA d'abast (no tocar ara):**
- Recuperar la contrasenya, verificar l'email, refresh tokens, OAuth, canviar la contrasenya o l'àlies, esborrar el compte.
- Gestió d'usuaris des del web (promocionar a admin es fa amb `createadmin`).
- Moderar o esborrar valoracions d'altres; watchlist ("+ A la meva llista"); "Novetats" / "Top valorades".
- Mòbil (Fase 5) i desplegament del web (Fase 6).

## 5. Com es verifica (Definition of Done)

- [ ] `curl`: register → `201` amb token; el mateix email o àlies → `409`; login correcte → `200`; contrasenya incorrecta → `401` genèric; massa intents de login → `429`.
- [ ] `POST /api/movies` **sense token → `401`**, **amb token de `user` → `403`**, **amb token d'`admin` → `201`**. El mateix per a `PUT`, `DELETE` i `/poster`. Un token manipulat o caducat → `401`.
- [ ] Un esborrany no surt a `GET /api/movies` ni a la fitxa (`404`) sense token d'admin; amb token d'admin, sí.
- [ ] Una valoració amb token de `user` es desa amb `user_id` i la llista en mostra l'àlies (mai l'email); sense token continua sent anònima.
- [ ] `PUT /api/ratings/{id}`: l'autor → `200`; un altre usuari → `403`; sense token → `401`; valoració anònima → `403`.
- [ ] `go run ./cmd/createadmin …` crea l'admin sense que la contrasenya aparegui a l'historial de la shell ni al repo.
- [ ] Web: l'admin entra, veu el badge ADMIN, crea una pel·lícula **en esborrany amb pòster** (amb previsualització), la publica i la veu al catàleg públic.
- [ ] Web: pujar un GIF o un fitxer de > 5 MB mostra l'estat 05b sense arribar a pujar-lo.
- [ ] Web: un usuari registrat valora "Com a @àlies", ho veu associat al seu àlies i en pot editar la nota; un visitant continua valorant anònimament.
- [ ] Web: un `user` que obre `/admin` a mà no hi entra, i un `401` d'un token caducat tanca la sessió.
- [ ] `npm run build` sense errors; `go vet ./...` net; `docker build` correcte; el web funciona a 375px sense scroll horitzontal.
- [ ] PR obert cap a `main` amb descripció clara i el checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- **Un sol camp de rol** (`users.role`: `admin` | `user`) i cap taula de permisos (§3).
- **Mateix endpoint de valoració** per a anònims i registrats (§3, §4): la diferència és només si hi ha usuari al context. És el checkpoint de la fase.
- **Decisions que has de prendre i explicar:**
  - **Una valoració per usuari i pel·lícula?** El pla diu "editar la seva nota", que suggereix que sí. Opció recomanada: índex únic parcial `(movie_id, user_id) WHERE user_id IS NOT NULL` (migració `000002`), `409` si ja n'hi ha una, i el web passa a mode "edita la teva valoració". Els anònims no tenen aquest límit (no es poden identificar).
  - **Caducitat del JWT** (p. ex. 7 dies) i què passa en caducar (el web tanca la sessió). Sense refresh tokens.
  - **On es guarda el token al web** (`localStorage` vs. memòria vs. cookie `HttpOnly`): pros i contres, i per què tries el que tries.
  - **Àlies:** format permès (p. ex. `[a-z0-9_]{3,20}`), si es mostra amb `@` i si és únic sense distingir majúscules.
- **Seguretat bàsica:** bcrypt amb el cost per defecte; comparació de contrasenyes sempre amb `bcrypt.CompareHashAndPassword` (temps constant); el JWT es verifica fixant l'algorisme esperat (rebutja `alg: none` i altres algorismes); `JWT_SECRET` només per variable d'entorn.
- **Primer admin a Railway:** `createadmin` s'executa en local contra la BD de Railway amb la URL **pública** de Postgres (`DATABASE_PUBLIC_URL`), no amb la interna. Deixa-ho escrit al README per quan es desplegui.
- **El límit per IP de les valoracions** es manté també per als usuaris registrats.
- **Checkpoint d'aprenentatge de la fase:** entendre com un token distingeix els tres casos (anònim / `user` / `admin`) i com es protegeixen les rutes per rol; per què la protecció de veritat és a l'API i no al guard del web; i per què el mateix endpoint de valoració serveix anònims i registrats.
