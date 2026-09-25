package models

import (
	"errors"
	"strings"
	"time"
	"unicode/utf8"
)

// Límits de longitud: part de la "mitigació mínima d'abús" (§5, Cicle B).
// Un comentari de 2.000 caràcters és una ressenya llarga; més és spam.
const (
	MaxCommentLen     = 2000
	MaxAuthorLabelLen = 40
)

// Rating és una valoració tal com la retorna l'API (§4).
//
// UserID és un punter perquè pot ser null: null = valoració anònima. És
// l'única diferència entre una valoració anònima i una de registrada:
// mateixa taula, mateix endpoint.
//
// UserAlias és l'àlies públic de l'autor registrat (null si és anònima). Es
// mostra l'àlies, MAI l'email: l'email és una dada privada.
type Rating struct {
	ID          string    `json:"id"`
	MovieID     string    `json:"movie_id"`
	UserID      *string   `json:"user_id"`
	UserAlias   *string   `json:"user_alias"`
	Score       int       `json:"score"`
	Comment     *string   `json:"comment"`
	AuthorLabel *string   `json:"author_label"` // nom lliure d'un anònim; només cosmètic
	CreatedAt   time.Time `json:"created_at"`
}

// RatingInput és el cos de POST /api/movies/{id}/ratings.
type RatingInput struct {
	Score       int     `json:"score"`
	Comment     *string `json:"comment"`
	AuthorLabel *string `json:"author_label"`
}

// RatingUpdateInput és el cos de PUT /api/ratings/{id}: només la nota i el
// comentari. La pel·lícula i l'autor d'una valoració no canvien mai.
type RatingUpdateInput struct {
	Score   int     `json:"score"`
	Comment *string `json:"comment"`
}

// AsInput reutilitza la normalització i la validació de RatingInput.
func (in RatingUpdateInput) AsInput() RatingInput {
	return RatingInput{Score: in.Score, Comment: in.Comment}
}

// Normalize treu espais sobrants i converteix els textos buits en null: "no
// hi ha comentari" es guarda sempre igual (NULL), no de dues maneres.
func (in *RatingInput) Normalize() {
	in.Comment = trimOrNil(in.Comment)
	in.AuthorLabel = trimOrNil(in.AuthorLabel)
}

func trimOrNil(s *string) *string {
	if s == nil {
		return nil
	}
	t := strings.TrimSpace(*s)
	if t == "" {
		return nil
	}
	return &t
}

// Validate comprova la nota i les longituds. Comptem caràcters (runes), no
// bytes: "Òrbita" són 6 caràcters però 7 bytes en UTF-8.
func (in RatingInput) Validate() error {
	switch {
	case in.Score < 1 || in.Score > 10:
		return errors.New("score ha d'estar entre 1 i 10")
	case in.Comment != nil && utf8.RuneCountInString(*in.Comment) > MaxCommentLen:
		return errors.New("comment massa llarg (màxim 2000 caràcters)")
	case in.AuthorLabel != nil && utf8.RuneCountInString(*in.AuthorLabel) > MaxAuthorLabelLen:
		return errors.New("author_label massa llarg (màxim 40 caràcters)")
	}
	return nil
}
