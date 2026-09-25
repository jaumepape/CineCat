package storage

import (
	"errors"
	"fmt"
	"image"
	"image/jpeg"
	"io/fs"
	"os"
	"path/filepath"
)

// PosterFiles desa i esborra els fitxers dels pòsters al disc.
//
// Aquí viuen els BYTES; a la BD només hi va la RUTA (movies.poster_path).
// Dir és UPLOAD_DIR: en local una carpeta del projecte, a Railway un volum
// persistent. El codi no distingeix els dos casos.
type PosterFiles struct {
	Dir string
}

// postersSubdir és la subcarpeta dins d'UPLOAD_DIR. Separar-la deixa lloc
// per a altres tipus de fitxer en el futur sense barrejar-los.
const postersSubdir = "posters"

// Init crea UPLOAD_DIR/posters si no existeix (idempotent).
func (p PosterFiles) Init() error {
	return os.MkdirAll(filepath.Join(p.Dir, postersSubdir), 0o755)
}

// RelPath és la ruta que es guarda a la BD per a una pel·lícula. El nom el
// genera el servidor a partir de l'id: mai es fa servir el nom del fitxer que
// envia el client (podria contenir "../" o col·lidir amb un altre).
func RelPath(movieID string) string {
	return postersSubdir + "/" + movieID + ".jpg"
}

// AbsPath és on és el fitxer al disc.
func (p PosterFiles) AbsPath(movieID string) string {
	return filepath.Join(p.Dir, filepath.FromSlash(RelPath(movieID)))
}

// Save codifica la imatge com a JPEG i la desa com a posters/<id>.jpg,
// substituint el pòster anterior si n'hi havia. Retorna la ruta relativa.
//
// Escriptura atòmica: primer s'escriu a un fitxer temporal de la mateixa
// carpeta i després es fa Rename. El Rename és atòmic dins d'un mateix disc,
// així qui demani el pòster mentre es desa rep el vell o el nou sencer, mai
// un JPEG a mitges.
func (p PosterFiles) Save(movieID string, img image.Image) (string, error) {
	dir := filepath.Join(p.Dir, postersSubdir)
	tmp, err := os.CreateTemp(dir, ".upload-*.tmp")
	if err != nil {
		return "", fmt.Errorf("creant el fitxer temporal: %w", err)
	}
	// Si alguna cosa falla abans del Rename, no deixem el temporal pel mig.
	// Després del Rename aquest Remove no troba res i no fa res.
	defer os.Remove(tmp.Name())

	if err := jpeg.Encode(tmp, img, &jpeg.Options{Quality: 85}); err != nil {
		tmp.Close()
		return "", fmt.Errorf("codificant el JPEG: %w", err)
	}
	if err := tmp.Close(); err != nil {
		return "", fmt.Errorf("tancant el fitxer temporal: %w", err)
	}
	// CreateTemp crea el fitxer amb permisos 0600; el deixem llegible com
	// qualsevol altre fitxer estàtic.
	if err := os.Chmod(tmp.Name(), 0o644); err != nil {
		return "", err
	}
	if err := os.Rename(tmp.Name(), p.AbsPath(movieID)); err != nil {
		return "", fmt.Errorf("movent el pòster al seu lloc: %w", err)
	}
	return RelPath(movieID), nil
}

// Remove esborra el pòster d'una pel·lícula. Que no existeixi no és un error:
// moltes pel·lícules no en tenen.
func (p PosterFiles) Remove(movieID string) error {
	err := os.Remove(p.AbsPath(movieID))
	if errors.Is(err, fs.ErrNotExist) {
		return nil
	}
	return err
}
