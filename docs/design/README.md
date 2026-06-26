# Handoff: CineCat — Catàleg de pel·lícules amb valoracions

## Overview
CineCat és un catàleg de pel·lícules amb valoracions d'usuaris (inspirat en l'UX
de llocs com FilmAffinity, sense copiar-ne marca ni contingut). Té dos fluxos:

- **(A) Públic** — navegar el catàleg, veure la fitxa d'una pel·lícula i valorar-la
  (com a visitant anònim o com a usuari registrat amb àlies).
- **(B) Administració** — mantenir el catàleg: llistar, crear i editar pel·lícules,
  incloent-hi la pujada del pòster.

Hi ha versions **web** (públic + admin) i versions **mòbil** (només consum:
catàleg, fitxa i pantalla de valorar).

## About the Design Files
Els fitxers d'aquest paquet són **referències de disseny creades en HTML**
(Design Components — prototips que mostren l'aspecte i el comportament desitjats),
**no codi de producció per copiar directament**.

La feina és **recrear aquests dissenys HTML dins l'entorn del codebase de destí**
(React, Vue, SwiftUI, etc.) fent servir els seus patrons, llibreries i convencions
ja establerts. Si encara no hi ha entorn, tria el framework més adequat per al
projecte i implementa-hi els dissenys. Tracta l'HTML com a especificació visual,
no com a artefacte a desplegar.

> Nota tècnica: els prototips estan escrits com a "Design Components" (fitxers
> `.dc.html` amb un petit runtime propi). El que importa per a la implementació és
> el **resultat visual i de comportament**, no l'estructura d'aquests fitxers.

## Fidelity
**Alta fidelitat (hifi).** Colors, tipografia, espaiats i interaccions són finals i
intencionats. Recrea la UI de manera fidel fent servir les llibreries i patrons del
codebase. Els valors exactes (hex, mides, pesos) són a la secció **Design Tokens**.

---

## Sistema de disseny (resum)

- **Tema**: fosc ("cinema"), pensat perquè els pòsters destaquin.
- **Accent únic**: teal `#2DD4BF` — s'usa per a accions primàries, notes i estats
  actius. Mantingues una sola línia d'accent coherent a tot el producte.
- **Tipografia**: `Geist` (interfície, títols, cos) + `Geist Mono` (notes, anys,
  metadades, comptadors). Carregades des de Google Fonts.
- **Components reutilitzables clau** (han de ser idèntics arreu):
  1. **Targeta de pel·lícula** (`MovieCard`): pòster 2:3 + nota + títol + any.
  2. **Selector/visualitzador de nota 1–10** (`RatingSelector`).
- El **pòster** és element de primer ordre: proporció vertical de cartell (2:3) i
  sempre amb previsió de l'estat "sense pòster" (placeholder).

---

## Screens / Views

Cada pantalla del prototip està etiquetada (00–08). Mides de referència web: amplada
de marc **1320px** (login **1160px**); mòbil **380×812** dins un bisell de telèfon.

### 00 · Sistema de disseny (style tile)
- **Purpose**: referència del sistema (paleta, tipografia, targeta, selector, botons).
- No és una pantalla de l'app; serveix de guia d'implementació.

### 01 · Catàleg (web)
- **Purpose**: navegar i descobrir pel·lícules; cercar i filtrar.
- **Layout**:
  - Capçalera fixa (alçada 62px): logo "CineCat" a l'esquerra, nav (Catàleg actiu /
    Novetats / Top valorades), botó "Inicia sessió" a la dreta.
  - Subbarra: camp de cerca per títol (icona de lupa + input, alçada 44px) +
    desplegable de gènere. A sota, fila de xips de gènere (Tots actiu, Drama,
    Ciència-ficció, Thriller, Terror, Romanç, Aventura).
  - Fila de títol "Catàleg · 248 pel·lícules" + ordenació ("Millor valorades").
  - **Graella de targetes**: `grid-template-columns: repeat(5, 1fr); gap: 20px`,
    padding lateral 26px.
- **Components**: capçalera, cerca, filtre, `MovieCard` ×N.
- **Variants incloses**:
  - **01b · Sense resultats**: input amb consulta "zombis al Montseny", gènere
    "Terror", estat buit centrat (placeholder de pòster en dashed + lupa), missatge i
    botons "Esborra els filtres" (primari) / "Veure tot el catàleg" (ghost).
  - **01c · Càrrega**: spinner + "Carregant el catàleg…" i graella de 10 targetes
    skeleton (bloc pòster 2:3 + 2 línies) amb animació shimmer.

### 02 · Fitxa de pel·lícula (web)
- **Purpose**: veure metadades i valoracions; valorar.
- **Layout**:
  - Capçalera + breadcrumb ("Catàleg / Drama / La Ciutat Submergida").
  - **Hero** en flex amb `gap: 34px`:
    - Pòster gran (amplada 296px, proporció 2:3, radi 12px) amb gradient i títol a sota.
    - Info: xips de gènere, títol (38px/700), línia mono "2021 · 1h 52min · Dir. Aina
      Roca", bloc de nota mitjana (cercle 84px amb vora teal i número "7,8") + "1.243
      valoracions", botons "Valora aquesta pel·lícula" (primari) i "+ A la meva
      llista" (ghost), etiqueta SINOPSI + paràgraf.
  - **Secció Valoracions** (separada per vora superior):
    - Capçalera "Valoracions · 1.243" + ordenació.
    - **Formulari de valorar** (targeta `#16181D`): "Deixa la teva valoració" +
      `RatingSelector` + textarea de ressenya opcional + fila amb segmented control
      Anònim / "Com a @joancinema", nota "No cal compte — pots valorar de manera
      anònima.", i botó "Envia la valoració".
    - **Llista de valoracions**: per ítem → avatar amb inicial (o "A" gris per a
      Anònim), nom (o "Anònim" en gris), xip de nota teal, data mono a la dreta, text.
- **Variant inclosa**:
  - **02b · Sense valoracions**: mateixa estructura però nota mitjana "—" (vora gris),
    "0 valoracions", i en lloc de llista un estat buit en dashed: "Encara no hi ha
    valoracions / Sigues la primera persona a valorar…". El formulari hi és igualment.

### 03 · Inici de sessió / Registre (web)
- **Purpose**: entrar o crear compte. **Iniciar sessió és OPCIONAL** per valorar.
- **Layout**: capçalera + bàner informatiu teal ("Iniciar sessió és opcional. Pots
  valorar de manera anònima…"), després dues targetes en `grid 1fr 1fr; gap: 24px`:
  - **Inicia sessió**: correu, contrasenya, enllaç "Has oblidat la contrasenya?",
    botó "Entra" (primari ple).
  - **Crea un compte**: àlies públic, correu, contrasenya (min. 8), botó "Crea el
    compte" (ghost amb vora teal).
  - Peu: "o bé continua com a visitant →".

### 04 · Admin — Llistat del catàleg (web)
- **Purpose**: gestionar el catàleg.
- **Layout**: capçalera amb badge "ADMIN" + chip d'usuari. Títol "Gestió del catàleg"
  + comptador, cerca i botó "+ Afegeix pel·lícula" (primari).
  - **Taula** dins contenidor amb vora i radi 12px. Columnes (grid):
    `54px 2.4fr 0.7fr 1.3fr 1.4fr 0.8fr 1fr 1fr` →
    miniatura pòster (34×51) · TÍTOL (+ "N vots") · ANY · DIRECCIÓ · GÈNERE · NOTA
    (teal) · ESTAT (xip "Publicada" teal / "Esborrany" gris) · ACCIONS (botó "Edita").
  - Capçalera de taula en mono, 7 files de mostra (una sense pòster i en esborrany).

### 05 · Admin — Crear / Editar pel·lícula (web)
- **Purpose**: alta/edició amb pujada de pòster.
- **Layout**: capçalera ADMIN + breadcrumb + títol + botons "Cancel·la" (ghost) /
  "Desa els canvis" (primari). Cos en `grid 1.5fr 1fr; gap: 28px`:
  - **Esquerra (camps)**: Títol; fila Any + Durada (min); Direcció; Gèneres (input de
    xips amb "×" + "Afegeix gènere"); Sinopsi (textarea).
  - **Dreta (pòster)**: previsualització 2:3 amb etiqueta "PREVISUALITZACIÓ", títol i
    botó "Canvia"; sota, zona de pujada en dashed: "Arrossega una imatge o navega" +
    nota mono "JPG o PNG · proporció 2:3 · màx. 5 MB".
- **Variant inclosa**:
  - **05b · Error de pujada**: zona de pujada en vermell (`#FF5252` translúcid) amb
    icona "!", "No s'ha pogut pujar la imatge", missatge "El fitxer és un GIF de 8,2
    MB. Fes servir JPG o PNG de fins a 5 MB." Chip del fitxer rebutjat
    ("zombis-montseny.gif · 8,2 MB · format no admès") amb vora vermella i "×". Botó
    "Desa" desactivat (opac, `cursor: not-allowed`).

### 06 · Catàleg mòbil
- **Purpose**: descobrir pel·lícules en pantalla estreta.
- **Layout** (dins telèfon 380×812): status bar (9:41, 5G, bateria), capçalera amb
  logo, camp de cerca, fila de xips de gènere (scroll horitzontal), **graella 2
  columnes** de `MovieCard` (`gap: 14px`), i **tab bar** inferior (Catàleg actiu /
  Cerca / Perfil) + home indicator.

### 07 · Fitxa mòbil
- **Purpose**: veure fitxa i valoracions; CTA per valorar.
- **Layout**: hero pòster (alçada 300px) amb gradient cap al fons, botó enrere "‹",
  xips de gènere, títol (24px), línia mono de metadades. Cos: bloc de nota (cercle
  62px "7,8" + "1.243 valoracions"), sinopsi, "Valoracions · 1.243" i 2 ressenyes
  compactes (una anònima). **CTA enganxat a baix**: botó ple "Valora aquesta
  pel·lícula" + home indicator.

### 08 · Valorar (mòbil)
- **Purpose**: posar nota i text, anònim o registrat.
- **Layout**: fulla inferior (bottom sheet) sobre fons enfosquit. Nansa (handle),
  fila amb miniatura de pòster + "VALORES / La Ciutat Submergida / 2021", "La teva
  nota" + `RatingSelector`, textarea opcional, "Publica com a" amb segmented control
  Anònim / @joancinema, botó ple "Envia la valoració" i nota "Pots valorar sense
  iniciar sessió".

---

## Components (especificació detallada)

### MovieCard (targeta de pel·lícula) — reutilitzable
- **Estructura**: contenidor pòster (relatiu, `aspect-ratio: 2/3`, `border-radius:
  8px`, `overflow: hidden`, vora `rgba(255,255,255,.07)`, ombra `0 8px 20px
  rgba(0,0,0,.4)`) + títol a sota (13.5px/600, una línia amb el·lipsi) + any (11.5px,
  Geist Mono, `#9298A1`).
- **Pòster amb imatge** (estat normal):
  - Fons = color del pòster (placeholder de mostra; veure paleta de pòsters).
  - Overlay: `linear-gradient(155deg, rgba(255,255,255,.12), transparent 42%),
    linear-gradient(0deg, rgba(0,0,0,.74), transparent 52%)`.
  - Micro-etiqueta "CARTELL" a dalt esq. (Geist Mono 8.5px, `letter-spacing:.14em`,
    `rgba(255,255,255,.5)`).
  - Títol sobreposat a baix (15px/600, blanc, text-shadow).
- **Pòster sense imatge** (placeholder "sense pòster"):
  - Fons `#1A1D22`, quadrat dashed 42×42 (`rgba(255,255,255,.22)`), text "SENSE
    PÒSTER" (Geist Mono 8.5px, `#5D636C`).
- **Xip de nota** (sempre, cantonada sup. dreta): fons `rgba(8,9,11,.82)`, vora
  `rgba(45,212,191,.32)`, número en teal `#2DD4BF` 12px/700 Geist Mono, radi 999px.
- **En producció**: substitueix el placeholder de color per la imatge real del pòster
  (`object-fit: cover`), mantenint l'overlay, el xip de nota i la proporció 2:3.

### RatingSelector (selector/visualitzador de nota 1–10) — reutilitzable
- **Capçalera**: número gran (38px/700, Geist Mono, teal) + "/ 10" (`#5D636C`) +
  etiqueta de paraula a la dreta (13px, `#9298A1`).
- **Etiquetes per valor**: 1 Horrible · 2 Molt dolenta · 3 Dolenta · 4 Fluixa ·
  5 Regular · 6 Acceptable · 7 Bona · 8 Molt bona · 9 Excel·lent · 10 Obra mestra.
  Sense selecció → número "0" i text "Tria una nota".
- **Segments**: 10 botons en fila (`display: flex; gap: 5px`), cadascun `flex: 1;
  height: 38px; border-radius: 8px`, Geist Mono 13px/500.
  - Inactiu: fons `#1D2026`, text `#7C828B`.
  - Actiu (índex ≤ valor): fons teal `#2DD4BF`, text `#06231F`.
  - Hover: el segment sota el cursor puja `translateY(-2px)` i la previsualització
    mostra el valor en hover (sense fixar-lo).
- **Estat**: `selected` (1–10 o cap) i `hover`. En clic es fixa `selected`. Accepta un
  valor per defecte opcional.

### Botons i xips
- **Primari**: fons `#2DD4BF`, text `#06231F`, `padding: ~11px 20px`, radi 9px, 600.
- **Ghost**: transparent, text `#EEF0F2`, vora `rgba(255,255,255,.16)`, radi 9px, 500.
- **Ghost-accent** (p. ex. "Crea el compte"): vora `rgba(45,212,191,.4)`.
- **Xip inactiu**: fons `#1D2026`, text `#9298A1`, vora `rgba(255,255,255,.08)`, radi
  999px. **Xip actiu**: fons `rgba(45,212,191,.15)`, text `#2DD4BF`, vora
  `rgba(45,212,191,.3)`.
- **Segmented control (Anònim / Registrat)**: contenidor `#0D0E11` (web) /`#16181D`
  (mòbil) amb vora i padding 3–4px; opció activa `rgba(45,212,191,.15/.16)` text teal,
  inactiva transparent text `#7C828B`.

### Inputs / camps
- Alçada 44px, fons `#0D0E11` (sobre superfície) o `#16181D`, vora
  `rgba(255,255,255,.1)`, radi 9–10px, text `#EEF0F2` 14px, placeholder `#5D636C`.
- Etiquetes de camp: 12px/500, `#9298A1`, marge inferior 7px.
- Textarea: mateixos estils, `resize: none`, `line-height: 1.5`.

---

## Interactions & Behavior
- **Cerca**: filtra el catàleg per títol (debounce recomanat). Sense coincidències →
  estat **sense resultats** (01b) amb botó per esborrar filtres.
- **Filtre de gènere**: xips o desplegable; combina amb la cerca.
- **Càrrega**: mentre es carrega el catàleg, mostra skeletons (01c) amb shimmer.
- **Valorar**: el `RatingSelector` actualitza la previsualització en hover i fixa la
  nota en clic. El segmented control alterna entre **Anònim** i **Registrat**; quan
  l'usuari no té sessió, "Registrat" pot estar deshabilitat o redirigir a login (però
  **valorar sense compte ha de ser possible**). Text de ressenya opcional.
- **Pujada de pòster**: accepta JPG/PNG fins a 5 MB, proporció 2:3 recomanada. Format
  o mida invàlids → estat **error** (05b): missatge clar, chip del fitxer rebutjat,
  botó "Desa" deshabilitat fins que hi hagi un pòster vàlid (o cap canvi pendent).
- **Transicions**: segments i tabs `transition: all .12s`. Hover de segment
  `translateY(-2px)`.
- **Navegació**: targeta → fitxa; "Valora" → formulari (web) / fulla de valorar
  (mòbil); "Edita"/"+ Afegeix" → formulari d'admin.

## State Management
- **Catàleg**: `query` (cerca), `genre` (filtre), `sort`, `status` (loading / loaded /
  empty / error), `movies[]`.
- **Fitxa**: `movie`, `reviews[]`, `avgRating`, `reviewCount`.
- **Formulari de valorar**: `selectedRating` (1–10 | null), `hoverRating`,
  `reviewText`, `publishMode` ('anon' | 'registered'), validació (nota obligatòria).
- **Admin form**: camps de la pel·lícula + `poster` (fitxer/URL) + `uploadError`.
- **Sessió**: `currentUser` (null per a visitant; àlies si registrat).

## Data model (de referència)
- **Pel·lícula**: `id, title, year, durationMin, director, genres[], synopsis,
  posterUrl|null, avgRating, ratingCount`.
- **Valoració**: `id, movieId, score (1–10), text?, author: {alias} | null (anònim),
  createdAt`.

## Design Tokens

### Colors
- Fons app: `#0D0E11`
- Superfície: `#16181D`
- Elevat / inactiu: `#1D2026`
- Vores: `rgba(255,255,255,.06–.12)`
- Text principal: `#EEF0F2`
- Text secundari: `#C8CCD2`
- Text mut: `#9298A1`
- Text feble: `#6B7177` / `#5D636C`
- **Accent / nota (teal)**: `#2DD4BF`; text sobre teal: `#06231F`
- Accent translúcid: `rgba(45,212,191,.12 / .15 / .3)`
- **Error**: `#FF5252` (vores/icona), text `#FF8A8A`, fons `rgba(255,82,82,.06)`
- **Colors de pòster (placeholders de mostra)**: pine `#2F4A3E`, navy `#2C3A5E`, plum
  `#5A3550`, maroon `#5B2F3A`, slate `#36404D`, rust `#6B3A2E`, indigo `#3B3A6B`, moss
  `#475036`, ochre `#6B5A2E`, "sense pòster" `#1A1D22`.

### Tipografia
- Famílies: **Geist** (UI/títols/cos), **Geist Mono** (notes, anys, metadades).
- Escala usada: títol fitxa 38/700; títols secció 19–21/700; cos 14–15/1.6; targeta
  títol 13.5/600; metadades 11.5–13.5 mono; nota gran 38/700 mono; etiquetes 12/500.
- `letter-spacing`: títols grans `-.02em`; etiquetes mono `.08–.2em`.

### Espaiat / radi / ombra
- Padding de pantalla web: 26–32px. Gaps de graella: 20px (web 5 col) / 14px (mòbil 2
  col).
- Radi: targetes 8px · panells 11–14px · botons/inputs 9–10px · xips/pills 999px ·
  telèfon (bisell) 46px / pantalla 36px.
- Ombres: targeta `0 8px 20px rgba(0,0,0,.4)`; panell de pantalla `0 24px 60px
  rgba(0,0,0,.45)`; pòster gran `0 16px 40px rgba(0,0,0,.5)`.

## Assets
- **Fonts**: Geist + Geist Mono via Google Fonts
  (`https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&family=Geist+Mono:wght@400;500`).
- **Pòsters**: al prototip són **placeholders de color** amb proporció 2:3 (no s'usen
  marques ni imatges reals). En producció, substitueix-los per pòsters reals amb
  `object-fit: cover` mantenint la proporció i l'estat "sense pòster".
- **Icones**: lupa, fletxa enrere, bateria, etc. estan dibuixades amb CSS bàsic al
  prototip; fes servir el set d'icones del codebase de destí.
- No s'usen logotips de tercers. El logo "CineCat" és un quadrat teal amb la lletra
  "C" + wordmark.

## Files
Prototips HTML (Design Components) en aquest paquet:
- `CineCat.dc.html` — totes les pantalles (00–08) en un llenç etiquetat.
- `MovieCard.dc.html` — component de targeta de pel·lícula.
- `RatingSelector.dc.html` — component de selector de nota 1–10.

Per veure'ls, obre `CineCat.dc.html` en un navegador (els altres dos s'hi munten com a
components).
