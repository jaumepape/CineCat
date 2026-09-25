# web/ — Frontend Vue 3 (web públic)

Catàleg i fitxa de pel·lícules amb valoració anònima. **Vue 3 + Vite + Vue Router + Pinia**, en JavaScript.

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
├── router.js          ← rutes: / (catàleg), /pelicula/:id (fitxa)
├── api/               ← ÚNIC lloc que fa fetch: una funció per endpoint
├── stores/catalog.js  ← Pinia: llista del catàleg en memòria entre pàgines
├── views/             ← pantalles (CatalogView, MovieView)
├── components/        ← peces reutilitzables (MovieCard, RatingSelector...)
├── styles/tokens.css  ← design tokens de docs/design com a variables CSS
└── utils/             ← format a la catalana ("7,8", "1h 52min") i gèneres
```

## Idees clau

- **Proxy de Vite:** `/api` i `/uploads` es reenvien al backend, així que per al navegador tot ve del mateix origen i les URL relatives dels pòsters funcionen tal qual.
- **Els filtres viuen a la URL** (`/?q=nit&genre=Drama`): es poden compartir i el botó enrere funciona. La llista carregada viu al store de Pinia, i per això tornar d'una fitxa al catàleg és instantani.
- **El web no calcula mai la mitjana:** després de valorar, torna a demanar la fitxa i l'API la recalcula amb SQL.
- **Valoració anònima:** `api/ratings.js` no envia cap capçalera `Authorization`, i l'API desa `user_id = NULL`.
