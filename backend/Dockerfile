# Dockerfile multi-etapa per a l'API Go de CineCat.
#
# Per què Docker (i no el builder automàtic Nixpacks de Railway)?
#  - El build queda definit al repo: el mateix Dockerfile construeix la
#    imatge en local i a Railway, així evitem el clàssic "a mi em funciona".
#  - Multi-etapa: compilem amb tot el toolchain de Go i després copiem
#    NOMÉS el binari a una imatge minúscula. La imatge final no porta el
#    compilador ni el codi font.

# --- Etapa 1: build ---------------------------------------------------------
FROM golang:1.23-alpine AS build
WORKDIR /src

# Copiem primer go.mod (i go.sum quan n'hi hagi) i descarreguem dependències
# en una capa separada. Així Docker reaprofita la cache mentre no canviïn les
# dependències, encara que canviï el codi.
COPY go.mod ./
RUN go mod download

# Ara el codi font i la compilació.
COPY . .
# CGO_ENABLED=0 -> binari estàtic, sense dependències de llibreries de C;
# així pot córrer en una imatge mínima. -ldflags "-s -w" treu informació de
# depuració per fer el binari més petit.
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /server ./cmd/server

# --- Etapa 2: runtime -------------------------------------------------------
FROM alpine:3.20
WORKDIR /app

# Copiem només el binari compilat de l'etapa anterior.
COPY --from=build /server /app/server

# Documenta el port; Railway de tota manera injecta PORT en temps d'execució.
EXPOSE 8080

ENTRYPOINT ["/app/server"]
