# CineCat — Documentació

Catàleg de pel·lícules amb valoracions d'usuaris (web + mòbil), inspirat en l'experiència de FilmAffinity només com a referència d'UX.

Projecte d'aprenentatge full-stack. **Prioritza claredat sobre completesa.**

## Stack (fix)

| Capa | Tecnologia |
|---|---|
| Backend / API | Go |
| Frontend web | Vue 3 |
| Base de dades | PostgreSQL |
| Mòbil (iOS + Android) | Kotlin Multiplatform (KMP) |
| Desplegament | Railway |

## Índex de documents

| Document | Contingut |
|---|---|
| [ESPECIFICACIO.md](ESPECIFICACIO.md) | Concepte, requisits, model de dades, API REST, arquitectura, stack, estructura de carpetes, pantalles i desplegament. **La font de veritat del disseny.** |
| [PLA-IMPLEMENTACIO.md](PLA-IMPLEMENTACIO.md) | Pla per fases (0→6) amb objectius, tasques concretes, checkpoints d'aprenentatge i criteris de verificació. **El que has de seguir per construir.** |

## Tres idees que has de recordar sempre

1. **La BD guarda rutes, el disc guarda bytes.** El concepte d'imatges més important del projecte.
2. **`ratings.user_id` nullable** modela anònim vs. registrat amb una sola taula i un sol contracte d'API.
3. **El volum persistent** de Railway és el que fa que les imatges sobrevisquin a un redeploy.
