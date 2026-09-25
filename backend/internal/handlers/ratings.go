package handlers

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"

	"github.com/jaumepape/cinecat/backend/internal/auth"
	"github.com/jaumepape/cinecat/backend/internal/models"
	"github.com/jaumepape/cinecat/backend/internal/storage"
)

// ratingsPageSize: valoracions per pàgina. Fix al servidor perquè cap client
// pugui demanar-ne 100.000 d'una vegada.
const ratingsPageSize = 20

// Ratings agrupa els handlers de valoracions.
type Ratings struct {
	Movies *storage.MovieStore
	Store  *storage.RatingStore
}

// GET /api/movies/{id}/ratings?page=
//
// Retorna un array (com diu §4). Si en torna menys de ratingsPageSize, era
// l'última pàgina: el client no necessita cap altra dada per saber-ho.
func (h Ratings) List(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}
	page := 1
	if v := r.URL.Query().Get("page"); v != "" {
		n, err := strconv.Atoi(v)
		if err != nil || n < 1 {
			writeError(w, http.StatusBadRequest, "page ha de ser un enter positiu")
			return
		}
		page = n
	}

	// Distingim "pel·lícula sense valoracions" (200 + []) de "pel·lícula que
	// no existeix" (404).
	if !h.movieVisible(w, r, id) {
		return
	}

	ratings, err := h.Store.List(r.Context(), id, page, ratingsPageSize)
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, ratings)
}

// movieVisible comprova que la pel·lícula existeix i que qui demana la pot
// veure (un esborrany només el veu l'admin). Si no, ja ha escrit el 404.
func (h Ratings) movieVisible(w http.ResponseWriter, r *http.Request, id string) bool {
	status, err := h.Movies.Status(r.Context(), id)
	if errors.Is(err, storage.ErrNotFound) || (err == nil && status == models.StatusDraft && !auth.IsAdmin(r.Context())) {
		writeError(w, http.StatusNotFound, "no trobat")
		return false
	}
	if err != nil {
		serverError(w, err)
		return false
	}
	return true
}

// POST /api/movies/{id}/ratings
//
// MATEIX endpoint i MATEIXA taula per a anònims i registrats. L'única
// diferència és si el middleware Authenticate ha posat un usuari al context:
//   - sense token → user_id = NULL (anònima; author_label opcional)
//   - amb token   → user_id = l'usuari (l'author_label s'ignora: ja té àlies)
func (h Ratings) Create(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}

	var in models.RatingInput
	if !decodeJSON(w, r, &in) {
		return
	}
	in.Normalize()
	if err := in.Validate(); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	if !h.movieVisible(w, r, id) {
		return
	}

	var userID *string
	if u := auth.FromContext(r.Context()); u != nil {
		userID = &u.ID
		in.AuthorLabel = nil
	}
	rating, err := h.Store.Create(r.Context(), id, userID, in)
	switch {
	case errors.Is(err, storage.ErrNotFound):
		writeError(w, http.StatusNotFound, "no trobat")
	case errors.Is(err, storage.ErrAlreadyRated):
		writeError(w, http.StatusConflict, err.Error())
	case err != nil:
		serverError(w, err)
	default:
		writeJSON(w, http.StatusCreated, rating)
	}
}

// PUT /api/ratings/{id}  {score, comment?}
//
// Només l'autor pot editar la seva valoració:
//   - anònim (sense token)       → 401
//   - valoració d'un altre       → 403
//   - valoració anònima          → 403 (no té autor: ningú no la pot reclamar)
func (h Ratings) Update(w http.ResponseWriter, r *http.Request) {
	u := auth.FromContext(r.Context())
	if u == nil {
		writeError(w, http.StatusUnauthorized, "no autenticat")
		return
	}
	id := chi.URLParam(r, "id")
	if !uuidRe.MatchString(id) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}

	var body models.RatingUpdateInput
	if !decodeJSON(w, r, &body) {
		return
	}
	in := body.AsInput()
	in.Normalize()
	if err := in.Validate(); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	current, err := h.Store.Get(r.Context(), id)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	if current.UserID == nil || *current.UserID != u.ID {
		writeError(w, http.StatusForbidden, "només pots editar les teves valoracions")
		return
	}

	rating, err := h.Store.Update(r.Context(), id, in)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, rating)
}
