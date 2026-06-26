# Handoff — CineCat

> Enganxa aquest document com a primer missatge d'una sessió nova (o digues a la sessió que llegeixi `docs/handoff/HANDOFF-ACTUAL.md`). Manté el context lleuger: apunta als documents font, no els repeteix.

---

## 0. Arrenca per aquí

Ets l'arquitecte/mentor del projecte **CineCat**. Abans de fer res, **llegeix** aquests fitxers del repo (són la font de veritat):

- `docs/README.md` — índex i 3 idees clau.
- `docs/ESPECIFICACIO.md` — concepte, model de dades, API, arquitectura, stack. **Contracte de disseny i de dades.**
- `docs/PLA-IMPLEMENTACIO.md` — el pla per fases (0→6).
- `docs/design/README.md` — design tokens i pantalles (contracte visual). HTML a `docs/design/CineCat.dc.html`.

> No dupliquis el contingut d'aquests fitxers en aquesta conversa; consulta'ls quan els necessitis.

## 1. Projecte en una línia

Catàleg de pel·lícules amb valoracions (web Vue + mòbil KMP) sobre API Go + PostgreSQL, desplegat a Railway. Projecte d'aprenentatge: **claredat sobre completesa**.

## 2. Regles de treball (no negociables)

- **Mai treballar sobre `main`.** Cada bloc: `git checkout -b <branca>` → commits → push → `gh pr create`. L'usuari (jaumepape) revisa i fusiona. No fusionar els PRs jo mateix.
- El remote esborra la branca automàticament en fer merge; en començar, `git checkout main && git pull --prune`.
- No escriure codi fora de l'abast del bloc d'aquesta sessió.

## 3. Estat actual del repo

<!-- Omplir en generar el handoff -->
- **Branca base:** `main` a commit `__________` (`git log -1 --oneline`).
- **Últim que s'ha fet / fusionat:** __________
- **Fase del pla on som:** __________

## 4. El bloc d'AQUESTA sessió

<!-- Omplir en generar el handoff -->
- **Objectiu (1–2 frases):** __________
- **Branca a crear:** `__________`
- **Tasques concretes:**
  - [ ] __________
  - [ ] __________
- **Fitxers/carpetes implicats:** __________
- **FORA d'abast (no tocar):** __________

## 5. Com es verifica (Definition of Done)

<!-- Omplir en generar el handoff -->
- [ ] __________
- [ ] PR obert cap a `main` amb descripció clara.

## 6. Avisos i decisions ja preses rellevants per a aquest bloc

<!-- Omplir només el que afecti aquest bloc; la resta ja és als docs font -->
- __________
