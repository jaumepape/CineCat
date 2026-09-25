// Package migrations conté els fitxers SQL versionats de la base de dades.
//
// Els fitxers s'incrusten dins del binari amb go:embed. Així la imatge Docker
// no necessita copiar la carpeta a part: el binari porta les seves pròpies
// migracions i sempre coincideixen amb la versió del codi.
package migrations

import "embed"

//go:embed *.sql
var FS embed.FS
