package auth

import (
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// Rols possibles (users.role).
const (
	RoleAdmin = "admin"
	RoleUser  = "user"
)

// User és la identitat que viatja dins del token i que els handlers llegeixen
// del context. Porta el mínim per decidir permisos sense anar a la BD.
type User struct {
	ID    string
	Role  string
	Alias string
}

// claims és el contingut del JWT. "sub" (subject) és l'id de l'usuari, "exp"
// la caducitat; role i alias són camps nostres.
type claims struct {
	Role  string `json:"role"`
	Alias string `json:"alias"`
	jwt.RegisteredClaims
}

// Tokens emet i verifica JWT signats amb HMAC-SHA256 (HS256).
//
// Un JWT són tres trossos en base64: capçalera.dades.signatura. Les dades NO
// estan xifrades (qualsevol les pot llegir): el que les fa fiables és la
// signatura, que només pot generar qui té el secret. Si algú canvia
// "role":"user" per "role":"admin", la signatura deixa de quadrar.
type Tokens struct {
	secret []byte
	ttl    time.Duration
}

// MinSecretLen: amb HS256, un secret curt es pot trobar per força bruta a
// partir d'un sol token. 32 bytes (256 bits) és el mínim raonable.
const MinSecretLen = 32

func NewTokens(secret string, ttl time.Duration) (*Tokens, error) {
	if len(secret) < MinSecretLen {
		return nil, fmt.Errorf("JWT_SECRET ha de tenir com a mínim %d caràcters", MinSecretLen)
	}
	return &Tokens{secret: []byte(secret), ttl: ttl}, nil
}

// Issue genera un token per a l'usuari, vàlid durant ttl.
func (t *Tokens) Issue(u User) (string, error) {
	now := time.Now()
	c := claims{
		Role:  u.Role,
		Alias: u.Alias,
		RegisteredClaims: jwt.RegisteredClaims{
			Subject:   u.ID,
			IssuedAt:  jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(t.ttl)),
		},
	}
	return jwt.NewWithClaims(jwt.SigningMethodHS256, c).SignedString(t.secret)
}

// ErrInvalidToken engloba qualsevol problema amb el token: mal format,
// signatura incorrecta, caducat... Al client només li diem "no autenticat".
var ErrInvalidToken = errors.New("token invàlid")

// Parse verifica la signatura i la caducitat, i retorna l'usuari.
//
// WithValidMethods fixa l'algorisme: un atac clàssic és enviar un token amb
// la capçalera "alg": "none" (sense signatura) o amb un altre algorisme
// esperant que el servidor se'l cregui. Només acceptem HS256.
func (t *Tokens) Parse(token string) (User, error) {
	var c claims
	_, err := jwt.ParseWithClaims(token, &c, func(*jwt.Token) (any, error) {
		return t.secret, nil
	}, jwt.WithValidMethods([]string{jwt.SigningMethodHS256.Alg()}), jwt.WithExpirationRequired())
	if err != nil || c.Subject == "" {
		return User{}, ErrInvalidToken
	}
	return User{ID: c.Subject, Role: c.Role, Alias: c.Alias}, nil
}
