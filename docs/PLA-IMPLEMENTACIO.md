# CineCat — Pla d'Implementació per Fases

> Guia accionable per construir el projecte. Llegeix abans [ESPECIFICACIO.md](ESPECIFICACIO.md). El disseny d'alta fidelitat (contracte visual) és a [docs/design/](design/); a les fases de frontend (3, 4, 5) recrea aquells dissenys fidelment, incloent-hi les variants d'estat (sense resultats, càrrega, sense valoracions, error de pujada).
>
> **Regla d'or:** cada fase ha de deixar **alguna cosa funcionant i verificable**. No passis a la fase següent fins que el "Com ho verifiques" estigui en verd i entenguis el "Checkpoint d'aprenentatge".

## Visió general de les fases

| Fase | Títol | Resultat tangible |
|---|---|---|
| 0 | Configuració i desplegament buit | `GET /health` viu a una URL de Railway |
| 1 | BD + API de catàleg (sense imatges) | CRUD de pel·lícules per API |
| 2 | Pujada i servei d'imatges | Pujar un pòster i veure'l al navegador |
| 3 | Frontend web públic | Catàleg + fitxa + valoració anònima al web |
| 4 | Auth + àrea d'administració | Login, rols i admin web amb pujada |
| 5 | Client mòbil KMP | App que navega el catàleg i valora |
| 6 | Ajust de desplegament i proves | Tot en prod, verificat end-to-end |

```
Fase 0 ──► Fase 1 ──► Fase 2 ──► Fase 3 ──► Fase 4 ──► Fase 5 ──► Fase 6
 infra      catàleg    imatges    web        admin      mòbil      prod
 buida      (API)      (pòster)   públic     + auth      (KMP)     final
```

---

## Fase 0 — Configuració i desplegament buit

**Objectiu:** muntar el "tub" sencer abans de posar-hi aigua. Vols veure codi teu corrent a una URL pública el primer dia.

**Tasques**
- [ ] Crear el repositori monorepo amb les carpetes `backend/`, `web/`, `mobile/`, `docs/`.
- [ ] Inicialitzar el mòdul Go a `backend/` i un servidor mínim amb un endpoint `GET /health` que retorni `200 OK`.
- [ ] Crear un projecte a Railway i afegir-hi el plugin **PostgreSQL** (encara no l'usaràs).
- [ ] Connectar el repo a Railway i desplegar el servei Go.
- [ ] Comprovar que la URL pública de Railway respon a `/health`.

**Com ho verifiques**
- `curl https://<la-teva-url>.railway.app/health` retorna `200`.

**Checkpoint d'aprenentatge**
- Entens com el codi va del teu git a una URL pública a Railway.
- Saps on es defineixen les variables d'entorn i com Railway injecta el `PORT`.

---

## Fase 1 — BD + API de catàleg (sense imatges)

**Objectiu:** el CRUD de pel·lícules i la lectura del catàleg, tot per API. Encara no hi ha imatges ni usuaris.

**Tasques**
- [ ] Escriure la primera migració amb les **3 taules** (`users`, `movies`, `ratings`) tal com defineix [ESPECIFICACIO.md §3](ESPECIFICACIO.md#3-model-de-dades). `poster_path` i `user_id` són nullable.
- [ ] Configurar la connexió a PostgreSQL llegint `DATABASE_URL`.
- [ ] Implementar endpoints de catàleg: `GET /api/movies` (amb `?q=` i `?genre=`), `GET /api/movies/{id}`, `POST /api/movies`, `PUT /api/movies/{id}`, `DELETE /api/movies/{id}`. *De moment sense protegir-los; l'auth arriba a la Fase 4.*
- [ ] Implementar el càlcul de `avg_score` i `rating_count` amb SQL (`AVG`, `COUNT`) dins de `GET /api/movies/{id}`.
- [ ] Afegir validació bàsica i els codis d'error `400`/`404`.
- [ ] Carregar unes quantes pel·lícules de prova (sense pòster encara).

**Com ho verifiques**
- Crear, llistar, consultar, editar i esborrar una pel·lícula amb `curl` o Postman.
- La fitxa retorna `avg_score: null` o `0` quan no hi ha valoracions.

**Checkpoint d'aprenentatge**
- Entens el flux **petició → handler → SQL → JSON**.
- Entens per què la mitjana es calcula i no es guarda duplicada.

---

## Fase 2 — Pujada i servei d'imatges (pòsters)

**Objectiu:** el cicle de vida complet d'una imatge. **És la fase conceptualment més important del projecte.**

**Tasques**
- [ ] Configurar `UPLOAD_DIR` (en dev: `backend/uploads`). Crear la subcarpeta `posters/`.
- [ ] Implementar `POST /api/movies/{id}/poster` que rep `multipart/form-data` (camp `file`):
  - [ ] Validar **mida** (≤ `MAX_UPLOAD_MB` = 5 MB) → `413` si excedeix.
  - [ ] Validar **tipus real** pels magic bytes (només JPG/PNG) → `415` si no.
  - [ ] Redimensionar a una amplada màxima (~500px).
  - [ ] Generar un nom segur (p. ex. `{id}.jpg`) i desar al volum.
  - [ ] `UPDATE movies SET poster_path=...`.
  - [ ] Retornar `{poster_url}`.
- [ ] Servir les imatges via `GET /uploads/posters/{file}` (fitxer estàtic des de `UPLOAD_DIR`).
- [ ] Fer que `GET /api/movies/{id}` retorni `poster_url` construïda a partir de `poster_path`.
- [ ] A Railway: afegir un **volum persistent** muntat a `/app/uploads`.

**Com ho verifiques**
- Pujar una imatge amb `curl -F "file=@poster.jpg"` retorna `{poster_url}`.
- Obrir `poster_url` al navegador mostra la imatge redimensionada.
- Pujar un fitxer massa gran dona `413`; un `.txt` renombrat a `.jpg` dona `415`.

**Checkpoint d'aprenentatge**
- Entens que **la BD guarda la ruta i el disc els bytes**, i per què.
- Entens per què validem mida i tipus (i per què pels magic bytes, no per l'extensió).
- Entens què és un volum persistent i per què cal.

---

## Fase 3 — Frontend web públic

**Objectiu:** un catàleg navegable i valorable des del navegador, encara sense login.

**Tasques**
- [ ] Inicialitzar el projecte Vue 3 + Vite a `web/` amb Vue Router i Pinia.
- [ ] Capa `api/` amb les funcions que criden l'API.
- [ ] Vista **Catàleg**: graella de pòsters, cerca per títol, filtre per gènere.
- [ ] Vista **Fitxa**: pòster, metadades, mitjana, nombre de vots, llista de ressenyes.
- [ ] Implementar endpoints de valoració: `GET /api/movies/{id}/ratings` i `POST /api/movies/{id}/ratings`.
- [ ] Formulari de valoració **anònima** (nota 1–10, text, `author_label` opcional), sense token.
- [ ] Després de valorar, refrescar la fitxa perquè la mitjana s'actualitzi.

**Com ho verifiques**
- Navegues pel catàleg, obres una fitxa i hi veus el pòster.
- Deixes una valoració anònima i la mitjana es recalcula.
- A la BD, la nova fila de `ratings` té `user_id = NULL`.

**Checkpoint d'aprenentatge**
- Entens com el web consumeix l'API.
- Entens com una ressenya anònima viatja fins a la BD amb `user_id = NULL` pel mateix endpoint que les registrades.

---

## Fase 4 — Autenticació i àrea d'administració

**Objectiu:** distingir els tres casos (anònim / registrat / admin) i protegir el manteniment del catàleg.

**Tasques**
- [ ] `POST /api/auth/register` i `POST /api/auth/login` amb hash bcrypt i emissió de JWT.
- [ ] Middleware que llegeix el `Bearer` token i resol l'usuari; distingeix anònim / `user` / `admin`.
- [ ] Protegir per rol els endpoints de catàleg (`POST/PUT/DELETE /movies` i `/poster` → només `admin`) → `401`/`403`.
- [ ] Permetre valoració **registrada**: si arriba token vàlid, `user_id` s'omple.
- [ ] `PUT /api/ratings/{id}`: només l'autor pot editar la seva valoració.
- [ ] Web: vistes de **Login/Registre**, **Admin — llistat** i **Admin — formulari** (crear/editar amb pujada de pòster i previsualització).
- [ ] Pinia: store d'auth que guarda token i usuari; afegir el token a les peticions protegides.
- [ ] Crear el primer usuari admin (script o INSERT manual amb hash).

**Com ho verifiques**
- Un `user` normal rep `403` en intentar crear una pel·lícula.
- L'admin entra, crea una pel·lícula i hi puja el pòster des de la web.
- Un usuari registrat valora, ho veu associat a ell i pot editar la seva nota.

**Checkpoint d'aprenentatge**
- Entens com un token distingeix els tres casos i com es protegeixen rutes per rol.
- Entens per què el mateix endpoint de valoració serveix anònims i registrats.

---

## Fase 5 — Client mòbil KMP

**Objectiu:** consumir **exactament la mateixa API** des del mòbil. Només consumidor (navegar + valorar).

**Tasques**
- [ ] Inicialitzar el projecte KMP a `mobile/` amb `shared/`, `androidApp/`, `iosApp/`.
- [ ] A `commonMain`: models (Movie, Rating, User), client **Ktor** + kotlinx.serialization, i les crides a l'API.
- [ ] Configurar el motor HTTP per plataforma (OkHttp a Android, Darwin a iOS).
- [ ] Pantalles **Catàleg**, **Fitxa** i **Valorar** (anònim o registrat).
- [ ] Càrrega del pòster des de `poster_url` (Coil a Android / Kingfisher a iOS, o loaders de Compose Multiplatform).

**Com ho verifiques**
- L'app llista el catàleg i mostra els pòsters servits per la mateixa API.
- Deixes una valoració des del mòbil i apareix també al web.

**Checkpoint d'aprenentatge**
- Entens què es comparteix (models, xarxa, lògica) i què és natiu (UI, seguretat, fitxers).
- Comproves que **una sola API** serveix dos clients diferents.

---

## Fase 6 — Ajust de desplegament i proves finals

**Objectiu:** tot en producció a Railway i verificat de punta a punta.

**Tasques**
- [ ] Servir el build de Vue des de l'API Go (o servei estàtic separat).
- [ ] Revisar totes les variables d'entorn de producció (`DATABASE_URL`, `JWT_SECRET`, `UPLOAD_DIR`, `MAX_UPLOAD_MB`).
- [ ] Confirmar que el volum persistent està muntat i que les imatges **sobreviuen a un redeploy**.
- [ ] Afegir la mitigació mínima d'abús per a valoracions anònimes (rate limit per IP).
- [ ] Prova end-to-end completa.

**Com ho verifiques (recorregut end-to-end)**
1. Admin web → crea pel·lícula → puja pòster.
2. El pòster es veu a la fitxa web.
3. Mòbil → el mateix pòster es carrega des de la mateixa URL.
4. **Redeploy** de l'API → el pòster segueix allà.
5. Visitant anònim valora → la mitjana s'actualitza a web i mòbil.

**Checkpoint d'aprenentatge**
- Entens com persisteixen les imatges entre desplegaments.
- Saps fer una prova completa de punta a punta amb tots dos clients.

---

## Apèndix — Ordre de dependències

```
Fase 1 (BD+API) ──┬─► Fase 2 (imatges) ──┐
                  │                       ├─► Fase 4 (auth+admin) ──► Fase 5 (mòbil) ──► Fase 6
                  └─► Fase 3 (web públic) ┘
```

- La Fase 3 (web públic) pot avançar en paral·lel a la Fase 2 mentre no necessitis mostrar pòsters reals.
- La Fase 4 depèn de tenir catàleg (1), imatges (2) i un web on enganxar l'admin (3).
- El mòbil (5) necessita el contracte d'API ja estable (fins a la 4).
