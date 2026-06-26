# CineCat — Especificació

> Document de disseny. No conté codi d'implementació a propòsit: l'objectiu és entendre **com encaixen les peces** i **com flueix una imatge** per tota la infraestructura.

---

## 1. CONCEPTE I ABAST

**Resum del producte.** CineCat és un catàleg web i mòbil de pel·lícules amb valoracions del públic. Un administrador manté el catàleg (crea pel·lícules amb pòster i metadades) des d'una àrea web protegida. Qualsevol visitant pot navegar pel catàleg i valorar una pel·lícula amb una nota de l'1 al 10 i un text opcional, ja sigui de forma anònima o havent-se registrat. La mateixa API REST alimenta tant el web (Vue) com el mòbil (Kotlin Multiplatform).

**Què queda FORA de l'MVP:**

| Fora de l'MVP | Per què |
|---|---|
| Galeria de múltiples imatges per pel·lícula | Una imatge (pòster) ja ensenya tot el cicle de vida d'una imatge. |
| Gènere com a entitat amb taula pròpia | Augmenta complexitat (relació N:M) sense ensenyar res nou. Serà un camp. |
| Edició/admin des del mòbil | El mòbil només consumeix. Mantenir l'abast acotat. |
| Recuperació de contrasenya, verificació d'email, OAuth | Autenticació "suficient per aprendre", no de producció. |
| Comentaris en fil, "m'agrada" a ressenyes, seguir usuaris | Xarxa social, fora del nucli. |
| Cerca avançada / paginació infinita / rendiment a escala | Una cerca per títol i un filtre per gènere basten. |
| CDN, processament d'imatge asíncron, thumbnails múltiples | Redimensionem una vegada en pujar i prou. |

---

## 2. REQUISITS FUNCIONALS (user stories)

**[MVP]** = imprescindible · **[+]** = desitjable si sobra temps.

### Flux A — Manteniment del catàleg (admin)

- **[MVP]** Com a **admin** vull iniciar sessió en una àrea protegida per gestionar el catàleg amb seguretat.
- **[MVP]** Com a **admin** vull crear una pel·lícula amb títol, any, durada, sinopsi i gènere per anar omplint el catàleg.
- **[MVP]** Com a **admin** vull pujar un pòster en crear o editar una pel·lícula perquè el catàleg sigui visual.
- **[MVP]** Com a **admin** vull editar les metadades i substituir el pòster d'una pel·lícula per corregir errors.
- **[+]** Com a **admin** vull esborrar una pel·lícula per treure contingut incorrecte.

### Flux B — Valoracions del públic

- **[MVP]** Com a **visitant** vull veure una graella de pòsters amb cerca per títol per trobar pel·lícules.
- **[MVP]** Com a **visitant** vull obrir la fitxa d'una pel·lícula (pòster, metadades, nota mitjana i ressenyes) per decidir si m'interessa.
- **[MVP]** Com a **visitant anònim** vull valorar (1–10) i deixar un text opcional sense crear compte, per la mínima fricció.
- **[MVP]** Com a **usuari registrat** vull valorar amb la meva identitat perquè la ressenya quedi associada a mi.
- **[MVP]** Com a **usuari registrat** vull veure i editar la meva pròpia valoració d'una pel·lícula per canviar d'opinió.
- **[+]** Com a **visitant** vull filtrar el catàleg per gènere per acotar la llista.

> La **nota mitjana** no és una entitat ni una columna mantinguda a mà; és un càlcul (`AVG`) que fa la base de dades quan demanes la fitxa. Guardar-la duplicada seria una font d'errors típica.

---

## 3. MODEL DE DADES

### Entitats: 3 (`users`, `movies`, `ratings`)

| Entitat | Per què existeix |
|---|---|
| `users` | Distingir admin i usuari registrat; associar ressenyes amb identitat. |
| `movies` | Nucli del catàleg; té el pòster. |
| `ratings` | Una pel·lícula té moltes valoracions; relació 1:N clara. |

### Taula `users`

| Columna | Tipus conceptual | Notes |
|---|---|---|
| `id` | UUID / serial (PK) | |
| `email` | text, únic | Login. |
| `alias` | text | **Àlies públic** ("@joancinema"); és el que es mostra a les ressenyes registrades, no l'email. |
| `password_hash` | text | Mai en clar (bcrypt). |
| `role` | text enum: `'admin'` \| `'user'` | **Aquí viu el rol d'admin**: un camp, no una entitat. |
| `created_at` | timestamp | |

### Taula `movies`

| Columna | Tipus conceptual | Notes |
|---|---|---|
| `id` | UUID / serial (PK) | |
| `title` | text | |
| `year` | integer | |
| `duration_min` | integer | Durada en minuts. |
| `director` | text | Direcció; es mostra a fitxa, taula admin i formulari. |
| `synopsis` | text | |
| `genres` | text[] (array) | **Gèneres en plural** (veure trade-off). Filtre: `WHERE 'Drama' = ANY(genres)`. |
| `status` | text enum: `'published'` \| `'draft'` | Estat editorial ("Publicada/Esborrany") de la taula admin. Per defecte `'draft'`. |
| `poster_path` | text, nullable | **Aquí viu el pòster**: la RUTA del fitxer, no els bytes. Nullable: es pot crear sense pòster. |
| `created_at` | timestamp | |

### Taula `ratings`

| Columna | Tipus conceptual | Notes |
|---|---|---|
| `id` | UUID / serial (PK) | |
| `movie_id` | FK → movies.id | A quina pel·lícula. |
| `user_id` | FK → users.id, **nullable** | **Aquí es modela anònim vs. registrat**: `NULL` = anònim. |
| `score` | integer 1–10 | Validat a l'API. |
| `comment` | text, nullable | Text opcional. |
| `author_label` | text, nullable | Nom lliure que un anònim pot posar; només cosmètic. |
| `created_at` | timestamp | |

### Representació de cada cosa clau

- **Pòster:** `movies.poster_path` guarda una referència curta (p. ex. `posters/a1b2.jpg`). Els **bytes NO** van a la BD.
- **Anònim vs. registrat:** una sola taula `ratings` amb `user_id` nullable.
  - `user_id` ple → ressenya registrada (editable pel seu autor).
  - `user_id` NULL → ressenya anònima (no editable; `author_label` opcional).
  - *Alternativa:* dues taules separades. **Rebutjada** perquè duplica la lògica de mitjana i de llistat.
- **Rol d'admin:** camp `users.role`. No cal taula de rols/permisos per a dos rols.

### Diagrama entitat-relació

```
        ┌─────────────────┐
        │      users      │
        ├─────────────────┤
        │ id        (PK)  │
        │ email           │
        │ alias           │  àlies públic
        │ password_hash   │
        │ role            │  'admin' | 'user'
        │ created_at      │
        └────────┬────────┘
                 │ 1
                 │ 0..N   (user_id NULLABLE → ressenya anònima)
        ┌────────┴────────┐
        │     ratings     │
        ├─────────────────┤        N         1   ┌─────────────────┐
        │ id        (PK)  │ ───────────────────► │     movies      │
        │ movie_id  (FK)  │                       ├─────────────────┤
        │ user_id   (FK?) │                       │ id        (PK)  │
        │ score (1-10)    │                       │ title           │
        │ comment    ?    │                       │ year            │
        │ author_label ?  │                       │ duration_min    │
        │ created_at      │                       │ director        │
        └─────────────────┘                       │ synopsis        │
                                                  │ genres   text[] │
          ?  = nullable                           │ status          │ 'published'|'draft'
                                                  │ poster_path  ?  │ ← ruta, no bytes
                                                  │ created_at      │
                                                  └─────────────────┘
```

### "Gènere": com es modela (decisió presa)

El disseny mostra **múltiples gèneres** per pel·lícula (xips + "Afegeix gènere"), així que un sol camp de text no servia. Opcions valorades:

| Opció | Avantatge | Inconvenient | Veredicte |
|---|---|---|---|
| Un sol camp de text | Mínim. | Contradiu el disseny (multi-gènere). | ❌ |
| **Columna array `genres text[]`** | Multi-gènere; **manté 3 entitats**; filtre senzill amb `ANY`. | Menys "normalitzat" que una taula. | ✅ **Triada** |
| Entitat `genres` + N:M | Multi-gènere "normalitzat". | Joins, més endpoints, 4a entitat; no ensenya domini nou. | ❌ Sobredimensiona |

**Decisió:** `movies.genres text[]`, validats contra una llista fixa al backend. Es manté el límit de 3 entitats i es respecta el contracte de disseny. Filtre de catàleg: `WHERE $1 = ANY(genres)`.

**Per què no calen més entitats:** tot el domini (qui ets, què mires, què opines) cap en aquestes tres. Una entitat es justifica quan té vida pròpia i relacions; si és un atribut, és un camp.

---

## 4. ESPECIFICACIÓ DE L'API REST

Un sol contracte per a web i mòbil. Base: `/api`.

| Mètode | Ruta | Qui | Descripció | Demana | Retorna |
|---|---|---|---|---|---|
| `POST` | `/api/auth/register` | públic | Crea usuari registrat | `{email, password}` | `{token, user}` |
| `POST` | `/api/auth/login` | públic | Login (user o admin) | `{email, password}` | `{token, user}` |
| `GET` | `/api/movies` | públic | Llista catàleg | query `?q=&genre=` | array movies (resum) |
| `GET` | `/api/movies/{id}` | públic | Fitxa + mitjana | — | movie + `avg_score` + `rating_count` |
| `POST` | `/api/movies` | **admin** | Crea pel·lícula | JSON metadades | movie creada |
| `PUT` | `/api/movies/{id}` | **admin** | Edita metadades | JSON metadades | movie actualitzada |
| `DELETE` | `/api/movies/{id}` | **admin** | Esborra | — | `204` |
| `POST` | `/api/movies/{id}/poster` | **admin** | **Puja/substitueix pòster** | `multipart/form-data` (`file`) | `{poster_url}` |
| `GET` | `/api/movies/{id}/ratings` | públic | Llista ressenyes | query `?page=` | array ratings |
| `POST` | `/api/movies/{id}/ratings` | públic o user | Crea valoració (anònima o registrada) | `{score, comment?, author_label?}` | rating creada |
| `PUT` | `/api/ratings/{id}` | **user (autor)** | Edita pròpia valoració | `{score, comment?}` | rating actualitzada |
| `GET` | `/uploads/posters/{file}` | públic | **Serveix la imatge** (bytes) | — | binari (image/jpeg) |

> Distinció clau d'imatges:
> - `POST /api/movies/{id}/poster` → **pujar** (multipart, protegit, admin).
> - `GET /uploads/posters/{file}` → **servir** (binari, públic, cacheable).

**Per què el pòster és un endpoint a part:** JSON net i provable; pots crear primer i pujar després; substituir el pòster sense tocar metadades. *Trade-off:* dues peticions en lloc d'una; per a un MVP educatiu la claredat guanya.

### Exemples de cos (JSON)

**Crear pel·lícula** — `POST /api/movies` (header `Authorization: Bearer <token-admin>`)
```json
// petició
{ "title": "El viatge", "year": 2021, "duration_min": 118,
  "director": "Aina Roca", "synopsis": "Un grup d'amics...",
  "genres": ["Drama", "Aventura"], "status": "draft" }

// resposta 201
{ "id": "a1b2", "title": "El viatge", "year": 2021, "duration_min": 118,
  "director": "Aina Roca", "synopsis": "Un grup d'amics...",
  "genres": ["Drama", "Aventura"], "status": "draft",
  "poster_url": null, "created_at": "2026-06-26T10:00:00Z" }
```

**Fitxa amb mitjana** — `GET /api/movies/a1b2`
```json
{ "id": "a1b2", "title": "El viatge", "year": 2021, "duration_min": 118,
  "director": "Aina Roca", "synopsis": "Un grup d'amics...",
  "genres": ["Drama", "Aventura"], "status": "published",
  "poster_url": "/uploads/posters/a1b2.jpg",
  "avg_score": 7.4, "rating_count": 12 }
```

**Valoració anònima** — `POST /api/movies/a1b2/ratings` (SENSE header d'auth)
```json
// petició
{ "score": 8, "comment": "M'ha agradat molt", "author_label": "Joan" }

// resposta 201 (user_id null → anònim; author_label és el nom cosmètic)
{ "id": "r99", "movie_id": "a1b2", "user_id": null,
  "score": 8, "comment": "M'ha agradat molt", "author_label": "Joan",
  "created_at": "2026-06-26T10:05:00Z" }
```

> Valoració **registrada**: si arriba `Authorization: Bearer <token>`, `user_id` s'omple i la resposta mostra l'`alias` de l'usuari (p. ex. `"@joancinema"`), no l'email.

### Pujada d'imatge (conceptual)

El navegador envia `POST` amb `Content-Type: multipart/form-data` i un part `file`. El backend Go: **valida** (mida + tipus MIME real pels magic bytes, no l'extensió), **redimensiona** (p. ex. amplada màx. 500px), **genera nom segur** (basat en l'`id` o hash), **desa al disc** (volum persistent) i **escriu `poster_path`** a la BD. Retorna la URL pública.

### Codis d'estat i errors

| Situació | Codi | Cos |
|---|---|---|
| Creat | `201` | l'objecte creat |
| OK sense cos (delete) | `204` | — |
| JSON invàlid / `score` fora 1–10 | `400` | `{error: "score ha d'estar entre 1 i 10"}` |
| Sense token / token invàlid | `401` | `{error: "no autenticat"}` |
| User normal vol crear pel·lícula | `403` | `{error: "cal rol admin"}` |
| Recurs inexistent | `404` | `{error: "no trobat"}` |
| **Imatge massa gran** | `413` | `{error: "màxim 5 MB"}` |
| **Format no permès** | `415` | `{error: "només JPG o PNG"}` |
| Error intern | `500` | `{error: "error del servidor"}` |

> Imatge: valida **mida** (màx. **5 MB** → `413`) i **tipus** pels magic numbers (JPG/PNG → `415`). No et fiïs de l'extensió. Proporció recomanada del pòster: **2:3** (cartell vertical).

---

## 5. ARQUITECTURA I INFRAESTRUCTURA

```
   ┌──────────────────┐        ┌──────────────────┐
   │  Client web      │        │  Client mòbil    │
   │  (Vue 3, SPA)    │        │  (KMP: iOS/And.) │
   └────────┬─────────┘        └────────┬─────────┘
            │  HTTPS / JSON              │  HTTPS / JSON
            │  + multipart (només admin)│
            └──────────────┬────────────┘
                           ▼
                ╔═══════════════════════════════════╗
                ║            RAILWAY                 ║
                ║   ┌────────────────────────────┐  ║
                ║   │   API (Go)                  │  ║
                ║   │  - rutes /api/*             │  ║
                ║   │  - auth (JWT)               │  ║
                ║   │  - valida + redimensiona    │  ║
                ║   │    imatges                  │  ║
                ║   │  - serveix /uploads/*       │  ║
                ║   └───────┬──────────────┬──────┘  ║
                ║           │ SQL          │ fitxers  ║
                ║           ▼              ▼          ║
                ║   ┌────────────┐  ┌───────────────┐║
                ║   │ PostgreSQL │  │ Volum         │║
                ║   │ (gestionat)│  │ persistent    │║
                ║   │            │  │ /uploads/...  │║
                ║   └────────────┘  └───────────────┘║
                ╚═══════════════════════════════════╝
```

### Cicle de vida A — Admin crea pel·lícula i puja pòster

```
1. Admin omple el formulari i prem "Crear".
2. Vue → POST /api/movies (JSON metadades, Bearer token admin).
3. Go valida token → role=admin → INSERT movies (poster_path=NULL) → {id:"a1b2", poster_url:null}.
4. Vue, amb l'id, envia el fitxer: POST /api/movies/a1b2/poster (multipart, "file").
5. Go:
     a. comprova mida (≤2MB) i tipus real (JPG/PNG) → si no, 413/415.
     b. redimensiona a ~500px d'amplada.
     c. genera nom "a1b2.jpg".
     d. desa bytes a /uploads/posters/a1b2.jpg (volum persistent).
     e. UPDATE movies SET poster_path='posters/a1b2.jpg' WHERE id='a1b2'.
     f. retorna {poster_url:"/uploads/posters/a1b2.jpg"}.
6. Vue mostra <img src="/uploads/posters/a1b2.jpg">
     → GET /uploads/posters/a1b2.jpg → Go llegeix del volum i retorna bytes.
```

La URL que en surt (`/uploads/posters/a1b2.jpg`) és estable i la fan servir igual web i mòbil.

### Cicle de vida B — Visitant anònim valora

```
1. Visitant obre la fitxa, posa nota (8) i text. NO té compte ni token.
2. Vue → POST /api/movies/a1b2/ratings (JSON, SENSE Authorization).
3. Go:
     a. no hi ha token → user_id = NULL (vàlid).
     b. valida score 1–10.
     c. mitigació mínima d'abús (rate limit per IP).
     d. INSERT ratings (movie_id='a1b2', user_id=NULL, score=8, ...).
4. Go → 201 amb la ressenya.
5. Vue refresca → GET /api/movies/a1b2 recalcula avg_score.
```

**Mitigació d'abús mínima:** *recomanat* rate limiting per IP + validació de `score` i longitud de `comment`. *Alternativa:* cookie/token anònim per limitar un vot per navegador (es burla esborrant cookies). No CAPTCHA ni moderació automàtica: sobredimensiona.

### KMP: compartit vs. específic

| Compartit (`commonMain`) | Específic per plataforma |
|---|---|
| Models (Movie, Rating, User) | UI nativa: Compose (Android) / SwiftUI (iOS) o Compose Multiplatform |
| Crida API + parsing JSON (Ktor + kotlinx.serialization) | Motor HTTP: OkHttp (Android) / Darwin (iOS) |
| Lògica de negoci/validació, estats de pantalla | Emmagatzematge segur del token (Keychain/Keystore) |
| URLs i contracte de l'API | Càrrega d'imatge segons llibreria nativa |

> Comparteixes la **lògica**; deixes natiu el que toca el SO (UI, seguretat, fitxers).

---

## 6. DETALL DEL STACK PER CAPA

### Backend (Go)

| Eina | Per a què | Per què |
|---|---|---|
| `net/http` + `chi` | Rutes i middleware | Lleuger, idiomàtic, llegible. |
| `pgx` (o `database/sql`+`lib/pq`) | Accés a PostgreSQL | Client modern; comença amb SQL pla per entendre les consultes. |
| `golang-jwt/jwt` | Tokens JWT | Estàndard de facto. |
| `golang.org/x/crypto/bcrypt` | Hash de contrasenyes | Mai en clar. |
| `disintegration/imaging` | **Redimensionar pòster** | Petita, sense deps pesades. |
| `mime/multipart` (estàndard) | **Rebre pujada** | Ja ve amb Go. |
| `golang-migrate` | Migracions de BD | Aprendre el concepte de migració versionada. |

### Frontend web (Vue 3)

| Eina | Per a què | Per què |
|---|---|---|
| Vue 3 + Vite | SPA i build | Estàndard actual, ràpid. |
| Vue Router | Navegació | Catàleg/fitxa/admin. |
| Pinia | Estat global (token, usuari) | Oficial, simple. |
| `fetch` o `axios` | Client HTTP | `fetch` basta; `axios` si vols interceptors per al token. |
| `<input type="file">` + `FormData` | **Pujada multipart** | Natiu, zero llibreries. |

### PostgreSQL

| Eina | Per a què |
|---|---|
| PostgreSQL gestionat de Railway | BD en producció. |
| `psql` / TablePlus / DBeaver | Inspeccionar dades mentre aprens. |

### Mòbil (KMP)

| Eina | Per a què | Per què |
|---|---|---|
| Ktor Client | **Client HTTP del mòbil** | Estàndard KMP; un codi per iOS i Android. |
| kotlinx.serialization | Parsing JSON | S'integra amb Ktor; comparteix models. |
| Coil (Android) / Kingfisher (iOS) | **Càrrega d'imatges** des de URL | Cache i càrrega asíncrona resolts. |
| Compose Multiplatform (opcional) | UI compartida | Si vols compartir també UI. |

---

## 7. ESTRUCTURA DE CARPETES

**Recomanació: monorepo.** Veus les tres parts alhora, fas commits coherents i comparteixes docs. *Trade-off:* en una empresa amb equips separats, repos independents donen desplegaments aïllats; aquí no compensa la fricció.

```
cinecat/
├── README.md                  ← visió general + com arrencar
├── docs/                      ← aquesta documentació
│
├── backend/                   ← API Go
│   ├── cmd/                   ← punt d'entrada (main)
│   ├── internal/
│   │   ├── handlers/          ← funcions per endpoint
│   │   ├── models/            ← structs del domini
│   │   ├── storage/           ← accés a BD i a fitxers d'imatge
│   │   └── auth/              ← JWT i middleware de rol
│   ├── migrations/            ← SQL versionat
│   └── uploads/               ← (DEV) imatges; EN PROD = volum
│       └── posters/           ← ★ AQUÍ VIUEN ELS PÒSTERS
│
├── web/                       ← frontend Vue
│   ├── src/
│   │   ├── views/             ← pantalles (catàleg, fitxa, admin, login)
│   │   ├── components/        ← peces reutilitzables (PosterCard, RatingForm)
│   │   ├── stores/            ← Pinia (auth, movies)
│   │   └── api/               ← funcions que criden l'API
│   └── public/
│
└── mobile/                    ← KMP
    ├── shared/                ← codi compartit iOS+Android
    │   └── commonMain/        ← models, client Ktor, lògica
    ├── androidApp/            ← UI i arrencada Android
    └── iosApp/                ← UI i arrencada iOS
```

> Imatges: `backend/uploads/posters/` en dev; un **volum persistent muntat a la mateixa ruta** en prod. El codi és idèntic; només canvia que en prod la carpeta és un volum.

---

## 8. PANTALLES PER MAQUETAR

> **El disseny d'alta fidelitat ja existeix** i és part del contracte: veure [docs/design/](design/) (handoff de Claude Design). Obre `docs/design/CineCat.dc.html` en un navegador per veure les pantalles 00–08 i els components. Els **design tokens** (colors, tipografia Geist/Geist Mono, accent teal `#2DD4BF`, espaiats, radis) són a [docs/design/README.md](design/README.md). El tema és **fosc** perquè els pòsters destaquin, i la proporció de pòster és **2:3**.
>
> **Components reutilitzables idèntics arreu:** `MovieCard` (pòster 2:3 + xip de nota + títol + any, amb estat "sense pòster") i `RatingSelector` (selector 1–10 amb etiquetes per valor).
>
> **Variants d'estat incloses al disseny** (cal implementar-les): catàleg *sense resultats* (01b) i *càrrega/skeleton* (01c); fitxa *sense valoracions* (02b); admin *error de pujada* (05b).
>
> **Elements del disseny FORA de l'MVP** (es maqueten però no s'implementen ara): "+ A la meva llista" (watchlist → seria 4a entitat) i la nav "Novetats / Top valorades" (són ordenacions del catàleg, no entitats noves).

### Web

| Pantalla | Propòsit | Elements clau |
|---|---|---|
| **Catàleg** | Descobrir pel·lícules | Graella de pòsters; cerca per títol; filtre per gènere; targeta amb pòster + títol + any + mitjana. |
| **Fitxa** | Veure detall i valorar | Pòster gran; metadades; mitjana + nombre de vots; llista de ressenyes; formulari (nota 1–10, text, nom opcional si anònim). |
| **Login / Registre** | Identificar-se | Camps email/contrasenya; commutar entrar/registrar-se. |
| **Admin — llistat** | Gestionar catàleg | Taula amb editar/esborrar; botó "Nova pel·lícula". |
| **Admin — formulari** | Crear/editar peli | Metadades; selector de gènere; **pujada de pòster** (input file + previsualització); desar. |

### Mòbil (KMP)

| Pantalla | Propòsit | Elements clau |
|---|---|---|
| **Catàleg (mòbil)** | Navegar | Llista/graella de pòsters amb cerca. |
| **Fitxa (mòbil)** | Veure i valorar | Pòster, metadades, mitjana, ressenyes, formulari. |
| **Valorar (mòbil)** | Posar nota | Selector 1–10, text opcional; anònim o registrat. |

> El mòbil **no** té pantalles d'admin (decisió d'abast).

---

## 9. PLA DE DESPLEGAMENT A RAILWAY

### Serveis

| Servei | Què és |
|---|---|
| **PostgreSQL** (plugin gestionat) | BD. Dona la `DATABASE_URL`. |
| **API Go** | Servei principal; compila des del repo, exposa URL pública. |
| **Frontend Vue** | **Opció A (recomanada):** servir el build estàtic des de la mateixa API Go (una sola URL, evita CORS). Opció B: servei estàtic separat. |

### On viuen les imatges en producció

**Recomanació: volum persistent muntat a `/app/uploads`, servit per l'API Go.**

| Opció | Avantatge | Inconvenient | Veredicte |
|---|---|---|---|
| **Volum persistent + servir des de Go** | Simple; controles validació i servei; sobreviu a redeploys; sense pagament. | Lligat a una instància; no escala a N rèpliques fàcilment. | ✅ Recomanat |
| Bytes a PostgreSQL (`bytea`) | Tot en un lloc; backup únic. | Infla la BD; consultes lentes; mala pràctica per a fitxers. | ❌ |
| Només URL externa (S3...) | Escala bé. | Servei extern (sovint de pagament); més conceptes. | ❌ MVP |

> **Punt crític:** el sistema de fitxers d'un contenidor és **efímer** — un redeploy l'esborra. Per això les imatges van en un **volum persistent**, que es manté entre desplegaments.

### Variables d'entorn

| Variable | Per a què |
|---|---|
| `DATABASE_URL` | Connexió a PostgreSQL (la dona Railway). |
| `JWT_SECRET` | Signar/verificar tokens. |
| `UPLOAD_DIR` | Ruta del volum, p. ex. `/app/uploads`. |
| `PORT` | Port d'escolta (Railway l'injecta). |
| `MAX_UPLOAD_MB` | Límit de mida del pòster (`5`). |

L'API llegeix `DATABASE_URL` a l'arrencada i obre el pool. Mai credencials al codi.

### Passos d'alt nivell

```
1. Crear projecte a Railway → afegir plugin PostgreSQL.
2. Connectar el repo; configurar build del servei Go.
3. Afegir VOLUM persistent muntat a /app/uploads (UPLOAD_DIR).
4. Definir variables d'entorn (JWT_SECRET, UPLOAD_DIR, MAX_UPLOAD_MB...).
5. Desplegar; verificar GET /health.
6. Executar migracions (les 3 taules).
7. Build de Vue → servir-lo des de l'API Go (o servei estàtic).
8. Crear el primer usuari admin (script o INSERT manual amb hash).
```

### Verificació end-to-end

1. Admin web → crea pel·lícula → puja pòster.
2. El pòster es veu a la fitxa web (`GET /uploads/posters/...`).
3. Mòbil → el mateix pòster es carrega des de la mateixa URL.
4. **Redeploy** de l'API → el pòster segueix allà → volum persistent OK.
5. Visitant anònim valora → la mitjana s'actualitza a web i mòbil.
