package handlers

import (
	"encoding/json"
	"errors"
	"net/http"
	"regexp"

	"github.com/go-chi/chi/v5"

	"github.com/jaumepape/cinecat/backend/internal/models"
	"github.com/jaumepape/cinecat/backend/internal/storage"
)

// Movies agrupa els handlers del catàleg. Rep el store per paràmetre
// (injecció de dependències): el handler no sap com s'ha obert la BD.
type Movies struct {
	Store *storage.MovieStore
}

// Routes registra les rutes de /api/movies. De moment són obertes; a la
// Fase 4 les d'escriptura quedaran darrere d'un middleware d'admin.
func (h Movies) Routes(r chi.Router) {
	r.Get("/", h.list)
	r.Post("/", h.create)
	r.Get("/{id}", h.get)
	r.Put("/{id}", h.update)
	r.Delete("/{id}", h.delete)
}

// uuidRe comprova el format d'un UUID. Si l'id de la URL no en té la forma,
// és impossible que existeixi: responem 404 directament en lloc de deixar
// que Postgres falli amb un error de sintaxi (que seria un 500).
var uuidRe = regexp.MustCompile(`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`)

// movieID llegeix {id} de la URL. Si no és vàlid, ja ha escrit el 404.
func movieID(w http.ResponseWriter, r *http.Request) (string, bool) {
	id := chi.URLParam(r, "id")
	if !uuidRe.MatchString(id) {
		writeError(w, http.StatusNotFound, "no trobat")
		return "", false
	}
	return id, true
}

// decodeMovieInput llegeix i valida el cos JSON de POST/PUT. Si falla, ja ha
// escrit el 400 amb el motiu.
func decodeMovieInput(w http.ResponseWriter, r *http.Request) (models.MovieInput, bool) {
	var in models.MovieInput
	// Límit de mida: un cos gegant no ens ha d'omplir la memòria.
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	dec := json.NewDecoder(r.Body)
	// Camps desconeguts → error. Així un error tipogràfic ("tittle") no
	// s'ignora en silenci.
	dec.DisallowUnknownFields()
	if err := dec.Decode(&in); err != nil {
		writeError(w, http.StatusBadRequest, "JSON invàlid: "+err.Error())
		return in, false
	}
	in.Normalize()
	if err := in.Validate(); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return in, false
	}
	return in, true
}

// GET /api/movies?q=&genre=
func (h Movies) list(w http.ResponseWriter, r *http.Request) {
	movies, err := h.Store.List(r.Context(), r.URL.Query().Get("q"), r.URL.Query().Get("genre"))
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, movies)
}

// GET /api/movies/{id}
func (h Movies) get(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}
	movie, err := h.Store.Get(r.Context(), id)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, movie)
}

// POST /api/movies
func (h Movies) create(w http.ResponseWriter, r *http.Request) {
	in, ok := decodeMovieInput(w, r)
	if !ok {
		return
	}
	movie, err := h.Store.Create(r.Context(), in)
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, movie)
}

// PUT /api/movies/{id}
func (h Movies) update(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}
	in, ok := decodeMovieInput(w, r)
	if !ok {
		return
	}
	movie, err := h.Store.Update(r.Context(), id, in)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, movie)
}

// DELETE /api/movies/{id}
func (h Movies) delete(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}
	err := h.Store.Delete(r.Context(), id)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
