# Handoff — CineCat · Fase 5 (client mòbil Kotlin Multiplatform)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — rellevants ara: **§4** (el contracte de l'API: és **exactament** el que ha de consumir el mòbil, inclosos `user_alias`, `409` i `429`), **§5 "KMP: compartit vs. específic"**, **§6 Mòbil** (Ktor, kotlinx.serialization, Coil/Kingfisher, Compose Multiplatform opcional), **§7** (`mobile/shared/commonMain`, `androidApp/`, `iosApp/`) i **§8 Mòbil** (el mòbil **no** té admin).
- `docs/design/README.md` — pantalles **06 Catàleg mòbil**, **07 Fitxa mòbil**, **08 Valorar (mòbil)** (bottom sheet), components **`MovieCard`** i **`RatingSelector`** i **Design Tokens** (colors, Geist/Geist Mono, graella de 2 columnes amb `gap: 14px`). Obre `docs/design/CineCat.dc.html` al navegador.
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 5.**

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-5-mobile` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix** si no t'ho demana explícitament.
- En començar: `git checkout main && git pull --prune`.
- **No toquis el backend ni el web** llevat que sigui imprescindible per al mòbil (i, si passa, explica-ho). L'objectiu és demostrar que **la mateixa API** serveix dos clients.
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.
- Mai credencials ni tokens al repo.

## 3. Estat actual del repo

- **Branca base:** `main` a `6f11792` (Merge PR #12). **Verifica-ho** amb `git log -1 --oneline` després del `git pull`.
- **Fet fins ara:**
  - **Backend** (Fases 0–4): API Go + PostgreSQL. Catàleg amb `avg_score`/`rating_count`, pòsters a `/uploads/posters/<id>.jpg` (**URL relativa**), valoracions anònimes o registrades (`user_alias`, una per usuari i pel·lícula → `409`, `PUT /api/ratings/{id}`), auth JWT (`/api/auth/register`, `/api/auth/login` → `{token, user}`, 7 dies), límit per IP (`429`). Els esborranys només els veu l'admin.
  - **Web** (Fases 3–4, `web/`, Vue): referència útil de com es consumeix l'API. Mira `web/src/api/*.js` (una funció per endpoint), `web/src/stores/auth.js` (sessió) i `web/src/utils/format.js` (format català: `7,8`, `1.243`, `1h 52min`).
- **`mobile/`** només conté un `README.md` placeholder. El `.gitignore` arrel ja ignora `.gradle/`, `/mobile/build/`, `/mobile/*/build/`, `local.properties`, `xcuserdata/`.
- **⚠️ Railway NO està desplegat** (serveis en estat REMOVED des del 26/06/2026). **Aquesta fase es fa contra el backend en local.** Per preparar-lo: Postgres amb Docker, `JWT_SECRET`, `go run ./cmd/createadmin`, `scripts/seed.sh` amb `TOKEN` i uns quants pòsters pujats (tot és al `README.md` arrel).
- **Entorn de la màquina (comprovat):**
  - **No hi ha cap JDK al `PATH`** (`java -version` falla). Hi ha el JBR d'Android Studio (OpenJDK 21): `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
  - Android SDK a `~/Library/Android/sdk` (plataforma `android-36.1`, system image `android-35`, emulador instal·lat).
  - Xcode 26.5 amb el runtime del simulador iOS 26.5.
  - `gradle` de Homebrew disponible, però fes servir el **Gradle wrapper** del projecte.
- **Fase del pla on som:** Fase 5 (la sisena).

## 4. El bloc d'AQUESTA sessió — Fase 5

**Objectiu:** una app mòbil (Android i iOS) que consumeix **exactament la mateixa API** que el web: navegar pel catàleg, veure la fitxa amb el pòster i valorar (anònim o registrat). Només consumeix: no hi ha res d'admin.

**Branca a crear:** `feat/fase-5-mobile`

**Ordre recomanat:** `shared` (models + client Ktor, provat contra l'API local) → **Android** (sencer i verificat a l'emulador) → **iOS**. Si el context de la sessió creix massa, tanca shared + Android en un PR i genera un handoff **5b** per a iOS.

### Tasques — `shared/` (`commonMain`)

- [ ] Projecte KMP a `mobile/` amb `shared/`, `androidApp/`, `iosApp/` (§7). Genera'l amb l'assistent de JetBrains (kmp.jetbrains.com) o a mà; fes servir versions estables **actuals** de Kotlin, Compose Multiplatform, Ktor i AGP, i comprova que són compatibles entre si.
- [ ] **Models** `@Serializable` que calquen §4: `Movie` (amb `avg_score`/`rating_count`), `Rating` (amb `user_id`, `user_alias`, `author_label`), `User` i `AuthResponse`. Noms JSON en snake_case (`@SerialName` o una estratègia de noms), i `ignoreUnknownKeys = true` perquè un camp nou de l'API no trenqui l'app.
- [ ] **Client Ktor** amb `ContentNegotiation` + JSON: `listMovies(q, genre)` (amb `status=published`), `getMovie(id)`, `listRatings(id, page)`, `createRating(...)`, `updateRating(...)`, `login(...)`, `register(...)`. Els errors `{error}` de l'API es converteixen en una excepció amb el missatge, com fa el web.
- [ ] **URL base configurable** per plataforma i entorn (vegeu §6). `poster_url` és **relativa**: el client hi ha d'afegir la URL base.
- [ ] **Motor HTTP per plataforma** (§5): OkHttp a `androidMain`, Darwin a `iosMain`.
- [ ] **Estat de les pantalles** compartit (ViewModel o similar a `commonMain`): catàleg (loading / loaded / empty / error), fitxa i formulari de valoració.
- [ ] Format català compartit (`7,8`, `1.243`, `1h 52min`) amb el mateix comportament que `web/src/utils/format.js`.

### Tasques — pantalles (Android i iOS)

- [ ] **06 Catàleg:** capçalera, cerca per títol (amb debounce), xips de gènere amb scroll horitzontal, **graella de 2 columnes** de `MovieCard` (pòster 2:3, xip de nota, títol, any, estat "sense pòster") i estats de càrrega, buit i error. Tab bar inferior (Catàleg / Cerca / Perfil).
- [ ] **07 Fitxa:** hero amb el pòster i un gradient, botó enrere, xips, títol, metadades en mono, bloc de nota (cercle de 62px), sinopsi i valoracions (àlies `@…` o "Anònim" / `author_label`). **CTA enganxat a baix**: "Valora aquesta pel·lícula".
- [ ] **08 Valorar:** **bottom sheet** amb una miniatura, `RatingSelector` 1–10 amb etiquetes, text opcional, "Publica com a" Anònim / `@àlies` (l'opció registrada només si hi ha sessió; si no, porta a iniciar sessió) i "Envia la valoració". En enviar, la fitxa es refresca (la mitjana la calcula l'API). Mostra els errors `400`/`409`/`429` de l'API.
- [ ] **Perfil (sessió):** inici de sessió (i registre, si hi ha temps) i "Surt". Sense sessió, el Perfil mostra el formulari; amb sessió, l'àlies. És el mínim per poder valorar "com a registrat" (el disseny no té una pantalla de login mòbil: fes-la simple amb els tokens del disseny).
- [ ] **Pòsters** carregats des de `baseUrl + poster_url` amb cache (Coil 3 si fas servir Compose Multiplatform; Coil / Kingfisher si la UI és nativa).
- [ ] **Token** desat de manera segura, amb codi específic per plataforma (`expect/actual`): Keystore a Android, Keychain a iOS (§5). Un `401` amb token tanca la sessió, com al web.

**Fitxers/carpetes implicats:** `mobile/` sencer (nou), `mobile/README.md` (com arrencar-ho), `README.md` arrel (estat i enllaç).

**FORA d'abast (no tocar ara):**
- Qualsevol pantalla d'admin (decisió d'abast, §8).
- Editar la valoració pròpia des del mòbil és **opcional**: només si queda temps (l'API ja ho permet: `PUT /api/ratings/{id}`).
- Mode offline, notificacions, deep links, publicar a les botigues, signar builds de release.
- Canvis a l'API o al web (vegeu §2).

## 5. Com es verifica (Definition of Done)

- [ ] `./gradlew :shared:allTests` (o equivalent) passa, amb com a mínim un test de deserialització d'un JSON real de l'API (movie amb `avg_score: null` i rating amb `user_alias`).
- [ ] **Android (emulador):** el catàleg llista les pel·lícules **publicades** amb els pòsters servits per l'API; la cerca i els gèneres filtren; la fitxa mostra la mitjana i les valoracions.
- [ ] **iOS (simulador):** el mateix.
- [ ] Una **valoració anònima** feta des del mòbil **apareix al web** (i a la BD amb `user_id = NULL`); la mitjana s'actualitza a tots dos clients.
- [ ] Amb sessió iniciada al mòbil, una valoració "com a `@àlies`" apareix amb l'àlies al web; una segona valoració de la mateixa pel·lícula mostra el missatge del `409`.
- [ ] Sense connexió amb l'API, l'app mostra un error amb "Torna-ho a provar", no es penja.
- [ ] PR obert cap a `main` amb descripció clara, captures d'Android i d'iOS i el checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- **Una sola API, dos clients:** el mòbil fa servir els mateixos endpoints, el mateix JSON i els mateixos codis d'error que el web. Si et cal alguna cosa que l'API no ofereix, pregunta-ho abans de canviar l'API.
- **URL de l'API en local:** des de l'**emulador d'Android**, `localhost` és l'emulador mateix: la màquina amfitriona és `http://10.0.2.2:8080`. Des del **simulador d'iOS**, `http://localhost:8080` sí que funciona.
- **HTTP en clar en local:** Android bloqueja `http://` per defecte (cal una `network_security_config` **només per a debug** que permeti `10.0.2.2`), i iOS també (App Transport Security: `NSAllowsLocalNetworking`). En producció, l'API anirà per HTTPS (Railway) i no caldrà cap excepció.
- **Decisions que has de prendre i explicar:**
  - **UI compartida (Compose Multiplatform) o nativa (Compose a Android + SwiftUI a iOS).** §6 ho deixa obert. Compose Multiplatform = una sola UI, molt menys codi i més ràpid; UI nativa = mostra millor la frontera "compartit vs. natiu" però duplica les pantalles. Recomanació: **Compose Multiplatform** per a la UI, i deixar com a codi de plataforma (`expect/actual`) el motor HTTP, l'emmagatzematge segur del token i la URL base: així el checkpoint de la fase segueix sent visible.
  - **Emmagatzematge del token:** quina llibreria o API de plataforma (Keystore/Keychain) i per què.
  - **Navegació** (Compose Navigation multiplatform o una solució més simple) i **ViewModel** (Lifecycle ViewModel multiplatform o classes pròpies amb `StateFlow`).
- **Fonts:** Geist i Geist Mono (Google Fonts, llicència OFL). Si les incrustes com a recursos, comprova'n la llicència i no les baixis de fonts no oficials.
- **Els esborranys no han d'aparèixer:** demana sempre `status=published` al catàleg, com fa el web.
- **Checkpoint d'aprenentatge de la fase:** entendre què es comparteix (models, xarxa, lògica i estat) i què és natiu (UI si no és CMP, motor HTTP, emmagatzematge segur, configuració de xarxa de cada SO); i comprovar que **una sola API** serveix dos clients diferents sense cap canvi.
