package models

import (
	"errors"
	"net/mail"
	"regexp"
	"strings"
	"time"
	"unicode/utf8"
)

// User és un usuari tal com el retorna l'API. Mai hi ha el password_hash:
// el hash no surt de la capa de storage.
type User struct {
	ID        string    `json:"id"`
	Email     string    `json:"email"`
	Alias     string    `json:"alias"`
	Role      string    `json:"role"`
	CreatedAt time.Time `json:"created_at"`
}

// Límits de contrasenya. El mínim de 8 és el del disseny. El màxim de 72 és
// una limitació de bcrypt: ignora tot el que passi de 72 bytes, així que és
// millor rebutjar-ho que acceptar una contrasenya que no es comprova sencera.
const (
	MinPasswordLen = 8
	MaxPasswordLen = 72
)

// aliasRe: l'àlies públic ("@joancinema"). Només minúscules, xifres i "_",
// de 3 a 20 caràcters: segur per mostrar i per posar en una URL, i sense
// dos àlies que només es diferenciïn per majúscules o accents.
var aliasRe = regexp.MustCompile(`^[a-z0-9_]{3,20}$`)

// RegisterInput és el cos de POST /api/auth/register.
type RegisterInput struct {
	Alias    string `json:"alias"`
	Email    string `json:"email"`
	Password string `json:"password"`
}

// Normalize passa email i àlies a minúscules: "Joan@Mail.com" i
// "joan@mail.com" han de ser el mateix compte. Treu també un "@" inicial de
// l'àlies, per si l'usuari l'escriu com es mostra.
func (in *RegisterInput) Normalize() {
	in.Email = strings.ToLower(strings.TrimSpace(in.Email))
	in.Alias = strings.ToLower(strings.TrimPrefix(strings.TrimSpace(in.Alias), "@"))
}

func (in RegisterInput) Validate() error {
	if !aliasRe.MatchString(in.Alias) {
		return errors.New("l'àlies ha de tenir de 3 a 20 caràcters: lletres minúscules, xifres o _")
	}
	if err := ValidateEmail(in.Email); err != nil {
		return err
	}
	return ValidatePassword(in.Password)
}

// ValidateEmail accepta només una adreça simple ("nom@domini"), sense
// "Nom <adreça>" ni altres formes que mail.ParseAddress també entendria.
func ValidateEmail(email string) error {
	addr, err := mail.ParseAddress(email)
	if err != nil || addr.Address != email || !strings.Contains(email, ".") {
		return errors.New("email invàlid")
	}
	return nil
}

func ValidatePassword(password string) error {
	if utf8.RuneCountInString(password) < MinPasswordLen {
		return errors.New("la contrasenya ha de tenir com a mínim 8 caràcters")
	}
	if len(password) > MaxPasswordLen {
		return errors.New("la contrasenya és massa llarga (màxim 72 bytes)")
	}
	return nil
}

// LoginInput és el cos de POST /api/auth/login.
type LoginInput struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

// AuthResponse és el que retornen register i login (§4).
type AuthResponse struct {
	Token string `json:"token"`
	User  User   `json:"user"`
}
