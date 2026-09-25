package handlers

import (
	"bytes"
	"errors"
	"fmt"
	"image"
	"image/color"
	"io"
	"io/fs"
	"net/http"
	"os"
	"regexp"

	"github.com/disintegration/imaging"
	"github.com/go-chi/chi/v5"

	"github.com/jaumepape/cinecat/backend/internal/storage"
)

const (
	// Amplada màxima del pòster desat. Amb proporció 2:3 dona ~500×750,
	// suficient per a la fitxa i lleuger per al mòbil.
	posterMaxWidth = 500
	// Límit de píxels ABANS de descodificar. Un PNG de pocs KB pot declarar
	// 50.000×50.000 píxels i, en descodificar-lo, ocupar gigues de memòria
	// (una "bomba de descompressió"). 40 Mpx ≈ 160 MB descodificats: prou per
	// a qualsevol fotografia real.
	posterMaxPixels = 40_000_000
	// Marge per a les capçaleres del multipart (límits, Content-Disposition...)
	// que viatgen al cos a més dels bytes del fitxer.
	multipartSlack = 64 << 10
)

// posterBackground és el fons de l'app (docs/design: #0D0E11). Un PNG amb
// transparència es compon sobre aquest color abans de passar-lo a JPEG, que
// no té canal alfa.
var posterBackground = color.NRGBA{0x0D, 0x0E, 0x11, 0xFF}

// Posters agrupa la pujada i el servei d'imatges.
type Posters struct {
	Store    *storage.MovieStore
	Files    storage.PosterFiles
	MaxBytes int64 // MAX_UPLOAD_MB en bytes
}

// POST /api/movies/{id}/poster  (multipart/form-data, camp "file")
//
// Recorregut: mida → tipus real → descodificar → redimensionar → desar
// bytes al disc → desar la ruta a la BD → retornar la URL pública.
func (h Posters) Upload(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}
	exists, err := h.Store.Exists(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}
	if !exists {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}

	data, status, msg := h.readFile(w, r)
	if status != 0 {
		writeError(w, status, msg)
		return
	}

	img, status, msg := decodePoster(data)
	if status != 0 {
		writeError(w, status, msg)
		return
	}

	rel, err := h.Files.Save(id, img)
	if err != nil {
		serverError(w, err)
		return
	}
	err = h.Store.SetPosterPath(r.Context(), id, rel)
	if errors.Is(err, storage.ErrNotFound) {
		// Algú ha esborrat la pel·lícula mentre pujàvem: no deixem el fitxer orfe.
		_ = h.Files.Remove(id)
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	// Mateixa construcció que fa storage al SELECT: '/uploads/' || poster_path.
	writeJSON(w, http.StatusOK, map[string]string{"poster_url": "/uploads/" + rel})
}

// readFile llegeix el camp "file" del multipart, amb el límit de mida.
// Retorna status 0 si tot va bé, o el codi i el missatge d'error.
//
// Per què MultipartReader i no ParseMultipartForm? ParseMultipartForm llegeix
// tot el formulari i desa les parts grans en fitxers temporals abans que
// puguem decidir res. Llegint part a part, tallem tan bon punt se supera el
// límit, i els bytes no toquen el disc fins que no els hem validat.
func (h Posters) readFile(w http.ResponseWriter, r *http.Request) ([]byte, int, string) {
	tooLarge := fmt.Sprintf("màxim %d MB", h.MaxBytes>>20)

	// Primera barrera: tot el cos de la petició. Si el client envia més,
	// MaxBytesReader talla la lectura (i tanca la connexió) en lloc de
	// deixar-nos llegir gigues.
	r.Body = http.MaxBytesReader(w, r.Body, h.MaxBytes+multipartSlack)

	mr, err := r.MultipartReader()
	if err != nil {
		return nil, http.StatusBadRequest, "cal un formulari multipart amb el camp 'file'"
	}
	for {
		part, err := mr.NextPart()
		if errors.Is(err, io.EOF) {
			return nil, http.StatusBadRequest, "falta el camp 'file'"
		}
		var maxErr *http.MaxBytesError
		if errors.As(err, &maxErr) {
			return nil, http.StatusRequestEntityTooLarge, tooLarge
		}
		if err != nil {
			return nil, http.StatusBadRequest, "formulari multipart invàlid"
		}
		if part.FormName() != "file" {
			continue // ignorem altres camps
		}

		// Segona barrera: el fitxer en si. Llegim com a màxim 1 byte més del
		// permès: si el llegim, sabem que el fitxer passa del límit.
		data, err := io.ReadAll(io.LimitReader(part, h.MaxBytes+1))
		if errors.As(err, &maxErr) || int64(len(data)) > h.MaxBytes {
			return nil, http.StatusRequestEntityTooLarge, tooLarge
		}
		if err != nil {
			return nil, http.StatusBadRequest, "no s'ha pogut llegir el fitxer"
		}
		return data, 0, ""
	}
}

// decodePoster valida que els bytes siguin de debò un JPEG o un PNG i els
// converteix en un pòster llest per desar (≤ 500px d'amplada, sense alfa).
func decodePoster(data []byte) (image.Image, int, string) {
	const badType = "només JPG o PNG"

	// Tipus REAL pels "magic bytes": els primers bytes de cada format són
	// fixos (JPEG: FF D8 FF; PNG: 89 50 4E 47...). El nom del fitxer i el
	// Content-Type els decideix el client i poden mentir; el contingut no.
	switch http.DetectContentType(data) {
	case "image/jpeg", "image/png":
	default:
		return nil, http.StatusUnsupportedMediaType, badType
	}

	// Mirem les dimensions declarades a la capçalera SENSE descodificar la
	// imatge sencera (barat), per evitar les bombes de descompressió.
	cfg, _, err := image.DecodeConfig(bytes.NewReader(data))
	if err != nil {
		return nil, http.StatusUnsupportedMediaType, badType
	}
	if cfg.Width*cfg.Height > posterMaxPixels {
		return nil, http.StatusRequestEntityTooLarge, "imatge massa gran (màxim 40 megapíxels)"
	}

	// AutoOrientation aplica la rotació EXIF: les fotos de mòbil sovint es
	// desen "de costat" amb una etiqueta que diu com girar-les.
	img, err := imaging.Decode(bytes.NewReader(data), imaging.AutoOrientation(true))
	if err != nil {
		// Capçalera correcta però contingut corrupte.
		return nil, http.StatusUnsupportedMediaType, badType
	}

	// Només reduïm: ampliar una imatge petita no n'afegeix detall, només pes.
	if img.Bounds().Dx() > posterMaxWidth {
		img = imaging.Resize(img, posterMaxWidth, 0, imaging.Lanczos) // 0 = manté la proporció
	}

	// Recodifiquem SEMPRE (també els JPEG): el fitxer desat el generem
	// nosaltres de zero, així que les metadades (EXIF amb GPS, etc.) i
	// qualsevol contingut amagat a l'original desapareixen.
	bg := imaging.New(img.Bounds().Dx(), img.Bounds().Dy(), posterBackground)
	return imaging.Overlay(bg, img, image.Pt(0, 0), 1.0), 0, ""
}

// posterFileRe: l'únic tipus de nom que pot existir a posters/ (<uuid>.jpg).
var posterFileRe = regexp.MustCompile(`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\.jpg$`)

// GET /uploads/posters/{file}
//
// Servim només noms que encaixen amb el patró <uuid>.jpg. Una llista blanca
// així fa impossible sortir d'UPLOAD_DIR amb "../" o demanar un altre fitxer,
// i de passada no hi ha llistat de directoris.
func (h Posters) Serve(w http.ResponseWriter, r *http.Request) {
	name := chi.URLParam(r, "file")
	if !posterFileRe.MatchString(name) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	f, err := os.Open(h.Files.AbsPath(name[:len(name)-len(".jpg")]))
	if errors.Is(err, fs.ErrNotExist) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	defer f.Close()
	info, err := f.Stat()
	if err != nil {
		serverError(w, err)
		return
	}

	// Cache: la URL d'un pòster no canvia quan se substitueix (<id>.jpg), així
	// que no el podem marcar com a "immutable" per un any. "no-cache" vol dir
	// "desa'l, però pregunta'm abans de reutilitzar-lo": el navegador envia
	// If-Modified-Since i, si no ha canviat, responem 304 sense bytes.
	// Alternativa descartada: posar la versió a la URL (?v=...) i cache
	// eterna; més eficient, però cal guardar la versió a la BD.
	w.Header().Set("Cache-Control", "no-cache")
	// Que el navegador no intenti endevinar el tipus: és image/jpeg i prou.
	w.Header().Set("X-Content-Type-Options", "nosniff")
	// ServeContent posa Content-Type (per l'extensió), Last-Modified i gestiona
	// If-Modified-Since (304) i les peticions per rangs.
	http.ServeContent(w, r, name, info.ModTime(), f)
}
