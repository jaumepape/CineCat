# Imatge única de CineCat: l'API Go + el web Vue ja compilat.
#
# Per què una sola imatge? §9 (Opció A): l'API serveix el web des del mateix
# origen → una sola URL, sense CORS, i les URL relatives dels pòsters
# funcionen tal qual. El context de build és l'ARREL del repo perquè cal tant
# web/ com backend/ (vegeu .dockerignore per al que NO s'hi copia).
#
# Multi-etapa: cada etapa té les eines que necessita (Node, Go) i la imatge
# final només porta el binari i els fitxers estàtics, sense compiladors ni
# node_modules.

# --- Etapa 1: web (Vue + Vite) -----------------------------------------------
FROM node:22-alpine AS web
WORKDIR /src/web
# Primer només els fitxers de dependències: mentre no canviïn, Docker
# reaprofita la capa de "npm ci" encara que canviï el codi.
COPY web/package.json web/package-lock.json ./
# npm ci instal·la EXACTAMENT el que diu package-lock.json (builds reproduïbles).
RUN npm ci
COPY web/ ./
RUN npm run build

# --- Etapa 2: API (Go) -------------------------------------------------------
FROM golang:1.26-alpine AS api
WORKDIR /src
COPY backend/go.mod backend/go.sum ./
RUN go mod download
COPY backend/ ./
# CGO_ENABLED=0 → binari estàtic, sense dependències de C: corre en una imatge
# mínima. -ldflags "-s -w" treu informació de depuració (binari més petit).
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /server ./cmd/server

# --- Etapa 3: runtime --------------------------------------------------------
FROM alpine:3.20
WORKDIR /app
COPY --from=api /server /app/server
COPY --from=web /src/web/dist /app/web

# L'API serveix el web des d'aquí (handlers.Web). Els pòsters van a
# /app/uploads, que a Railway és un VOLUM persistent: el disc del contenidor
# s'esborra a cada deploy, el volum no.
ENV WEB_DIR=/app/web \
    UPLOAD_DIR=/app/uploads

# Documenta el port; Railway injecta PORT en temps d'execució.
EXPOSE 8080

ENTRYPOINT ["/app/server"]
