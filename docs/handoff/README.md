# Handoffs entre sessions

Per mantenir cada sessió amb el context lleuger, treballem **un bloc per sessió**. En acabar (o quan calgui), es genera un **handoff**: un document curt que permet arrencar una sessió nova fresca sense arrossegar tot l'historial.

## Filosofia

- El handoff **apunta** als documents font (`docs/ESPECIFICACIO.md`, `docs/PLA-IMPLEMENTACIO.md`, `docs/design/`), **no els duplica**. Així el context nou es manté petit i no hi ha dues fonts de veritat.
- Un handoff descriu **un sol bloc de feina** (típicament una fase o sub-fase del pla).
- Tot el que sigui durador (decisions, contracte, model de dades) va als documents font, no al handoff. El handoff és efímer.

## Com s'usa

1. Quan ho demanis, genero un handoff a partir de [TEMPLATE.md](TEMPLATE.md) amb l'estat real del repo i el bloc següent.
2. El desem com `docs/handoff/HANDOFF-ACTUAL.md` (sempre el mateix nom: només importa l'últim).
3. Obres una **sessió nova** i, com a primer missatge, hi enganxes el contingut del handoff (o li dius que llegeixi `docs/handoff/HANDOFF-ACTUAL.md`).
4. La sessió nova fa la feina del bloc, en branca pròpia + PR.

## Recordatori del flux git (sempre)

Mai treballar sobre `main`. Cada bloc: branca nova → commits → push → PR perquè el reviseu i fusioneu. El remote esborra la branca automàticament en fer merge.
