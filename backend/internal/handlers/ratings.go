package handlers

import (
	"encoding/json"
	"errors"
	"net/http"
	"strconv"

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
	exists, err := h.Movies.Exists(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}
	if !exists {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}

	ratings, err := h.Store.List(r.Context(), id, page, ratingsPageSize)
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, ratings)
}

// POST /api/movies/{id}/ratings
//
// Sense token → valoració anònima (user_id = NULL). A la Fase 4, si arriba un
// token vàlid, aquí mateix s'omplirà userID: MATEIX endpoint, MATEIXA taula.
func (h Ratings) Create(w http.ResponseWriter, r *http.Request) {
	id, ok := movieID(w, r)
	if !ok {
		return
	}

	var in models.RatingInput
	r.Body = http.MaxBytesReader(w, r.Body, 64<<10)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&in); err != nil {
		writeError(w, http.StatusBadRequest, "JSON invàlid: "+err.Error())
		return
	}
	in.Normalize()
	if err := in.Validate(); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	var userID *string // anònim; la Fase 4 l'omplirà a partir del token
	rating, err := h.Store.Create(r.Context(), id, userID, in)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, rating)
}
