package handlers

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/jaumepape/cinecat/backend/internal/auth"
	"github.com/jaumepape/cinecat/backend/internal/models"
	"github.com/jaumepape/cinecat/backend/internal/storage"
)

// Auth agrupa el registre i l'inici de sessió.
type Auth struct {
	Users  *storage.UserStore
	Tokens *auth.Tokens
}

// decodeJSON llegeix un cos JSON petit i estricte (camps desconeguts → error).
// Si falla, ja ha escrit el 400.
func decodeJSON(w http.ResponseWriter, r *http.Request, v any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, 64<<10)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(v); err != nil {
		writeError(w, http.StatusBadRequest, "JSON invàlid: "+err.Error())
		return false
	}
	return true
}

// respondWithToken emet el token i respon {token, user}.
func (h Auth) respondWithToken(w http.ResponseWriter, status int, u models.User) {
	token, err := h.Tokens.Issue(auth.User{ID: u.ID, Role: u.Role, Alias: u.Alias})
	if err != nil {
		serverError(w, err)
		return
	}
	writeJSON(w, status, models.AuthResponse{Token: token, User: u})
}

// POST /api/auth/register  {alias, email, password}
//
// El rol NO ve del client: tothom qui es registra és 'user'. Si el rol es
// llegís del cos, qualsevol es podria fer admin enviant "role":"admin".
// (Com que DisallowUnknownFields està actiu, un camp "role" dona 400.)
func (h Auth) Register(w http.ResponseWriter, r *http.Request) {
	var in models.RegisterInput
	if !decodeJSON(w, r, &in) {
		return
	}
	in.Normalize()
	if err := in.Validate(); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	hash, err := auth.HashPassword(in.Password)
	if err != nil {
		serverError(w, err)
		return
	}
	user, err := h.Users.Create(r.Context(), in.Email, in.Alias, hash, auth.RoleUser)
	if errors.Is(err, storage.ErrEmailTaken) || errors.Is(err, storage.ErrAliasTaken) {
		writeError(w, http.StatusConflict, err.Error())
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	h.respondWithToken(w, http.StatusCreated, user)
}

// POST /api/auth/login  {email, password}
//
// Tant si l'email no existeix com si la contrasenya és incorrecta, la
// resposta és la mateixa (i triga el mateix): no volem que el login serveixi
// per esbrinar quins emails estan registrats.
func (h Auth) Login(w http.ResponseWriter, r *http.Request) {
	var in models.LoginInput
	if !decodeJSON(w, r, &in) {
		return
	}
	in.Email = normalizeEmail(in.Email)

	user, hash, err := h.Users.GetByEmail(r.Context(), in.Email)
	if errors.Is(err, storage.ErrNotFound) {
		auth.CheckPasswordDummy(in.Password)
		writeError(w, http.StatusUnauthorized, "email o contrasenya incorrectes")
		return
	}
	if err != nil {
		serverError(w, err)
		return
	}
	if !auth.CheckPassword(hash, in.Password) {
		writeError(w, http.StatusUnauthorized, "email o contrasenya incorrectes")
		return
	}
	h.respondWithToken(w, http.StatusOK, user)
}

func normalizeEmail(email string) string {
	in := models.RegisterInput{Email: email}
	in.Normalize()
	return in.Email
}
