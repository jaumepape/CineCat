package auth

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
)

type ctxKey struct{}

// FromContext retorna l'usuari de la petició, o nil si és anònima.
func FromContext(ctx context.Context) *User {
	u, _ := ctx.Value(ctxKey{}).(*User)
	return u
}

// IsAdmin és una drecera per als handlers.
func IsAdmin(ctx context.Context) bool {
	u := FromContext(ctx)
	return u != nil && u.Role == RoleAdmin
}

// Authenticate llegeix "Authorization: Bearer <token>" i posa l'usuari al
// context. Distingeix els tres casos:
//   - sense capçalera        → anònim (vàlid: es pot navegar i valorar);
//   - token vàlid            → user o admin, segons el rol del token;
//   - token invàlid/caducat  → 401. Qui envia un token espera estar
//     identificat: tractar-lo en silenci com a anònim amagaria l'error.
//
// NO decideix permisos: només diu QUI ets. Què pots fer ho decideix
// RequireRole (o el handler) a cada ruta.
func (t *Tokens) Authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		header := r.Header.Get("Authorization")
		if header == "" {
			next.ServeHTTP(w, r)
			return
		}
		token, ok := strings.CutPrefix(header, "Bearer ")
		if !ok {
			writeError(w, http.StatusUnauthorized, "no autenticat")
			return
		}
		user, err := t.Parse(token)
		if err != nil {
			writeError(w, http.StatusUnauthorized, "no autenticat")
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), ctxKey{}, &user)))
	})
}

// RequireRole deixa passar només els usuaris amb aquest rol.
//   - anònim     → 401 (no sabem qui ets: identifica't)
//   - altre rol  → 403 (sabem qui ets, però no tens permís)
func RequireRole(role string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			u := FromContext(r.Context())
			if u == nil {
				writeError(w, http.StatusUnauthorized, "no autenticat")
				return
			}
			if u.Role != role {
				writeError(w, http.StatusForbidden, "cal rol "+role)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

// writeError: mateix format {"error": "..."} que la resta de l'API.
func writeError(w http.ResponseWriter, status int, msg string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]string{"error": msg})
}
