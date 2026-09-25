#!/usr/bin/env bash
# Carrega pel·lícules de prova (les del disseny) fent POST a l'API.
#
# Per què per l'API i no amb un INSERT SQL? Així el seed passa per la mateixa
# validació que qualsevol client i, de passada, prova els endpoints. I no va
# dins de les migracions perquè les dades de prova no han d'arribar mai a
# producció sense voler-ho.
#
# Crear pel·lícules és només per a admins: cal el token d'un admin a TOKEN
# (el retorna POST /api/auth/login).
#
# Ús:
#   TOKEN=$(curl -s -X POST $API_URL/api/auth/login -H 'Content-Type: application/json' \
#     -d '{"email":"…","password":"…"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
#   API_URL=http://localhost:8080 TOKEN=$TOKEN ./scripts/seed.sh
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
: "${TOKEN:?cal TOKEN amb el token d’un admin (veure la capçalera d’aquest script)}"

post() {
  curl -sS -f -X POST "$API_URL/api/movies" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' -d "$1" -o /dev/null -w "%{http_code} "
  echo "$1" | sed -E 's/.*"title": *"([^"]*)".*/\1/'
}

post '{"title":"La Ciutat Submergida","year":2021,"duration_min":112,"director":"Aina Roca","synopsis":"Una arqueòloga descobreix que el poble inundat de la seva infància amaga un secret.","genres":["Drama","Misteri"],"status":"published"}'
post '{"title":"Òrbita Baixa","year":2023,"duration_min":127,"director":"Marc Vidal","synopsis":"Tres astronautes queden atrapats en una estació orbital a la deriva.","genres":["Ciència-ficció"],"status":"published"}'
post '{"title":"Cor de Tramuntana","year":2024,"duration_min":98,"director":"Jordi Ferran","synopsis":"Un amor d’estiu a l’Empordà que el vent no deixa oblidar.","genres":["Romanç","Drama"],"status":"published"}'
post '{"title":"Nits de Neó","year":2022,"duration_min":105,"director":"Pau Serra","synopsis":"Un taxista nocturn es veu embolicat en una xarxa de xantatges.","genres":["Thriller"],"status":"published"}'
post '{"title":"L’Última Sessió","year":2022,"duration_min":91,"director":"Clara Mas","synopsis":"Un cinema de barri tanca per sempre, però alguna cosa no vol marxar.","genres":["Terror"],"status":"draft"}'
post '{"title":"Mapes del No-Res","year":2023,"duration_min":119,"director":"Oriol Pi","synopsis":"Dos germans segueixen un mapa antic pel Pirineu.","genres":["Aventura","Drama"],"status":"published"}'
post '{"title":"Vidres Trencats","year":2021,"duration_min":110,"director":"Sergi Aloy","synopsis":"Un inspector jubilat reobre el cas que mai va poder tancar.","genres":["Drama","Crim"],"status":"published"}'
