# web/ — Frontend Vue 3 (web públic)

Catàleg i fitxa de pel·lícules amb valoracions (anònimes o amb àlies), inici de sessió i àrea d'administració. **Vue 3 + Vite + Vue Router + Pinia**, en JavaScript.

## Arrencar

Cal el backend corrent a `http://localhost:8080` (veure el [README arrel](../README.md)).

```bash
npm install
npm run dev      # servidor de desenvolupament a http://localhost:5173
npm run build    # build de producció a dist/
```

## Estructura

```
src/
├── main.js            ← crea l'app i hi connecta Router i Pinia
├── router.js          ← rutes + guard: /, /pelicula/:id, /entra, /admin/**
├── api/               ← ÚNIC lloc que fa fetch: una funció per endpoint; hi afegeix el token
├── stores/auth.js     ← Pinia: sessió (token + usuari) desada a localStorage
├── stores/catalog.js  ← Pinia: llista del catàleg en memòria entre pàgines
├── views/             ← pantalles (Catalog, Movie, Login, AdminList, AdminForm)
├── components/        ← peces reutilitzables (MovieCard, RatingSelector, PosterPicker...)
├── styles/tokens.css  ← design tokens de docs/design com a variables CSS
└── utils/             ← format a la catalana ("7,8", "1h 52min") i gèneres
```

## Idees clau

- **Proxy de Vite:** `/api` i `/uploads` es reenvien al backend, així que per al navegador tot ve del mateix origen i les URL relatives dels pòsters funcionen tal qual.
- **Els filtres viuen a la URL** (`/?q=nit&genre=Drama`): es poden compartir i el botó enrere funciona. La llista carregada viu al store de Pinia, i per això tornar d'una fitxa al catàleg és instantani.
- **El web no calcula mai la mitjana:** després de valorar, torna a demanar la fitxa i l'API la recalcula amb SQL.
- **Anònim o registrat, mateix endpoint:** `api/ratings.js` envia el token només si l'usuari tria "Com a @àlies". Sense capçalera `Authorization`, l'API desa `user_id = NULL`.
- **El guard de rutes és comoditat, no seguretat:** amaga `/admin` a qui no és admin, però qui protegeix de debò és l'API (`401`/`403`).
- **Sessió caducada:** si l'API respon `401` a una petició amb token, es tanca la sessió i les lectures es repeteixen sense token.
- **Pòster:** es valida i es previsualitza al navegador (`URL.createObjectURL`) abans de pujar-lo; desar són dues peticions (metadades i després el pòster).
