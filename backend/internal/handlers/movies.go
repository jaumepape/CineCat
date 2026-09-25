package handlers

import (
	"errors"
	"log"
	"net/http"
	"regexp"

	"github.com/go-chi/chi/v5"

	"github.com/jaumepape/cinecat/backend/internal/auth"
	"github.com/jaumepape/cinecat/backend/internal/models"
	"github.com/jaumepape/cinecat/backend/internal/storage"
)

// Movies agrupa els handlers del catàleg. Rep el store per paràmetre
// (injecció de dependències): el handler no sap com s'ha obert la BD.
type Movies struct {
	Store   *storage.MovieStore
	Posters Posters
}

// Routes registra les rutes de /api/movies. Llegir és públic; escriure
// (crear, editar, esborrar, pujar pòster) només ho pot fer un admin.
//
// La protecció és AQUÍ, a l'API. Que el web amagui els botons d'admin és
// només comoditat: qualsevol pot fer un curl saltant-se el web.
func (h Movies) Routes(r chi.Router) {
	r.Get("/", h.list)
	r.Get("/{id}", h.get)

	r.Group(func(r chi.Router) {
		r.Use(auth.RequireRole(auth.RoleAdmin))
		r.Post("/", h.create)
		r.Put("/{id}", h.update)
		r.Delete("/{id}", h.delete)
		r.Post("/{id}/poster", h.Posters.Upload)
	})
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
	// decodeJSON limita la mida del cos i rebutja camps desconeguts: així un
	// error tipogràfic ("tittle") no s'ignora en silenci.
	if !decodeJSON(w, r, &in) {
		return in, false
	}
	in.Normalize()
	if err := in.Validate(); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return in, false
	}
	return in, true
}

// GET /api/movies?q=&genre=&status=
//
// Els esborranys només els veu l'admin. Per a qualsevol altre, el filtre
// status=published s'imposa al servidor, digui el que digui la URL: una
// petició amb ?status=draft sense ser admin retorna una llista buida.
func (h Movies) list(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	status := query.Get("status")
	if status != "" && status != models.StatusDraft && status != models.StatusPublished {
		writeError(w, http.StatusBadRequest, "status ha de ser 'draft' o 'published'")
		return
	}
	if !auth.IsAdmin(r.Context()) {
		if status == models.StatusDraft {
			writeJSON(w, http.StatusOK, []models.MovieDetail{})
			return
		}
		status = models.StatusPublished
	}
	movies, err := h.Store.List(r.Context(), query.Get("q"), query.Get("genre"), status)
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
	// Un esborrany, per a qui no és admin, "no existeix": 404 i no 403, per
	// no revelar que hi ha una pel·lícula amb aquest id encara per publicar.
	if errors.Is(err, storage.ErrNotFound) || (err == nil && movie.Status == models.StatusDraft && !auth.IsAdmin(r.Context())) {
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
	// La fila ja no existeix: esborrem també el fitxer del pòster perquè no
	// quedi orfe al disc. Si falla, la pel·lícula ja està esborrada; només
	// ho registrem (un fitxer sobrant no trenca res).
	if err := h.Posters.Files.Remove(id); err != nil {
		log.Printf("esborrant el pòster de %s: %v", id, err)
	}
	w.WriteHeader(http.StatusNoContent)
}
