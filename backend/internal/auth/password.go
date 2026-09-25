// Package auth conté tot el que té a veure amb identitat: contrasenyes
// (bcrypt), tokens (JWT) i els middlewares que decideixen qui pot fer què.
package auth

import "golang.org/x/crypto/bcrypt"

// HashPassword retorna el hash bcrypt d'una contrasenya. És el que es guarda
// a users.password_hash; la contrasenya en clar no es guarda mai.
//
// Per què bcrypt i no SHA-256? bcrypt és LENT a propòsit (cost ajustable) i
// afegeix una sal aleatòria a cada hash. Si algun dia es filtrés la BD,
// provar milions de contrasenyes per força bruta seria caríssim, i dues
// persones amb la mateixa contrasenya tenen hashes diferents.
func HashPassword(password string) (string, error) {
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	return string(hash), err
}

// CheckPassword diu si la contrasenya correspon al hash. La comparació és de
// temps constant: no filtra per cronometratge quants caràcters coincideixen.
func CheckPassword(hash, password string) bool {
	return bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) == nil
}

// dummyHash és un hash vàlid d'una contrasenya qualsevol. Quan l'email no
// existeix, el login compara igualment contra aquest hash perquè la resposta
// trigui el mateix que si existís: així no es pot esbrinar per temps de
// resposta quins emails estan registrats.
var dummyHash, _ = HashPassword("cinecat-dummy-password")

// CheckPasswordDummy gasta el mateix temps que CheckPassword i retorna false.
func CheckPasswordDummy(password string) {
	_ = CheckPassword(dummyHash, password)
}
