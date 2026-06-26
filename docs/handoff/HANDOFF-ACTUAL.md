# Handoff — CineCat · Fase 0 (infra buida + `GET /health`)

> Enganxa aquest document com a primer missatge d'una sessió nova, o digues a la sessió: *"llegeix `docs/handoff/HANDOFF-ACTUAL.md` i comença"*. Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases. **Aquest bloc és la Fase 0.**
- `docs/ESPECIFICACIO.md` — §6 (stack Go) i §7 (estructura de carpetes) són els rellevants ara. La resta, consulta-la quan calgui.

> No dupliquis el contingut d'aquests fitxers a la conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Aquest bloc: crea la branca `feat/fase-0-health` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. **No fusionar el PR tu mateix.**
- En començar: `git checkout main && git pull --prune`. El remote esborra la branca automàticament en fer merge.
- No escriguis codi fora de l'abast d'aquest bloc.
- Aquest és un projecte d'aprenentatge: explica el **perquè** de cada decisió, no només el què.

## 3. Estat actual del repo

- **Branca base:** `main` a commit `392425d` (verifica amb `git log -1 --oneline`).
- **Fet fins ara:** només documentació (PR #1 contracte+disseny, PR #2 plantilla de handoff). **No hi ha gens de codi encara.**
- **Fase del pla on som:** Fase 0 (la primera). Existeix `docs/`; falten `backend/`, `web/`, `mobile/`.

## 4. El bloc d'AQUESTA sessió — Fase 0

**Objectiu:** muntar el "tub" sencer abans de posar-hi aigua: un esquelet de backend Go que respongui `GET /health → 200`, llest per desplegar a Railway. Vols veure codi teu corrent a una URL pública com abans millor.

**Branca a crear:** `feat/fase-0-health`

**Tasques concretes:**
- [ ] Crear l'arbre del monorepo segons `ESPECIFICACIO.md §7`: carpetes `backend/`, `web/`, `mobile/` (les dues últimes poden quedar amb un `.gitkeep` o un README mínim de moment).
- [ ] Inicialitzar el mòdul Go a `backend/` (`go mod init`).
- [ ] Servidor HTTP mínim amb un endpoint `GET /health` que retorni `200` (idiomàtic; pots usar `net/http` directament o `chi` segons `§6`, llegint el `PORT` de l'entorn perquè Railway l'injecta).
- [ ] Afegir la configuració de desplegament que Railway necessiti (p. ex. fitxer de config / Dockerfile o equivalent), explicant l'opció triada.
- [ ] Actualitzar el `README.md` arrel (o crear-lo) amb instruccions mínimes per arrencar el backend en local.
- [ ] (Manual de l'usuari, documentar els passos) Crear el projecte a Railway, afegir-hi el plugin PostgreSQL i connectar el repo. **No cal fer-ho des de la sessió; deixa-ho documentat per a l'usuari.**

**Fitxers/carpetes implicats:** `backend/` (nou), `web/` i `mobile/` (placeholders), `README.md` arrel.

**FORA d'abast (no tocar ara):**
- Cap taula ni connexió a la BD (això és la Fase 1; el plugin PostgreSQL s'afegeix però no s'usa).
- Cap endpoint de domini (`/movies`, `/ratings`...).
- Res de frontend ni de mòbil (només placeholders de carpeta).
- Imatges/pòsters (Fase 2).

## 5. Com es verifica (Definition of Done)

- [ ] `go run` (o `go build` + executar) al `backend/` aixeca el servidor i `curl localhost:<PORT>/health` retorna `200`.
- [ ] El servidor llegeix el port de la variable d'entorn `PORT` (amb un valor per defecte raonable per a local).
- [ ] Hi ha la config de desplegament perquè Railway pugui construir i arrencar el servei.
- [ ] El `README.md` arrel explica com arrencar-lo en local i quins passos manuals queden a Railway.
- [ ] PR obert cap a `main` amb descripció clara del que s'ha fet i del checkpoint d'aprenentatge.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

- Stack backend ja decidit (`§6`): Go amb router `chi` (o `net/http` per al simple `/health`), `pgx` per a la BD (encara no s'usa).
- `.gitignore` ja cobreix Go, Node, KMP i `backend/uploads/`. No el toquis si no cal.
- Variables d'entorn previstes (`§9`): `PORT`, i més endavant `DATABASE_URL`, `JWT_SECRET`, `UPLOAD_DIR`, `MAX_UPLOAD_MB`. A la Fase 0 només cal `PORT`.
- **Checkpoint d'aprenentatge de la fase:** entendre com el codi va del git a una URL pública a Railway i com s'injecten les variables d'entorn (sobretot `PORT`).
