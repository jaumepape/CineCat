// Package models conté els structs del domini i les seves regles de validació.
//
// No sap res d'HTTP ni de SQL: així la mateixa regla ("l'any ha de ser
// raonable") serveix tant si la dada arriba per l'API com per un script.
package models

import (
	"errors"
	"fmt"
	"slices"
	"strings"
	"time"
)

// Genres és la llista tancada de gèneres acceptats (decisió §3: text[]
// validat al backend, sense taula de gèneres). Surt del disseny.
var Genres = []string{
	"Acció", "Animació", "Aventura", "Ciència-ficció", "Comèdia", "Crim",
	"Documental", "Drama", "Misteri", "Romanç", "Terror", "Thriller",
}

// Valors possibles de Movie.Status.
const (
	StatusDraft     = "draft"
	StatusPublished = "published"
)

// Movie és una pel·lícula tal com la retorna l'API.
type Movie struct {
	ID          string    `json:"id"`
	Title       string    `json:"title"`
	Year        int       `json:"year"`
	DurationMin int       `json:"duration_min"`
	Director    string    `json:"director"`
	Synopsis    string    `json:"synopsis"`
	Genres      []string  `json:"genres"`
	Status      string    `json:"status"`
	PosterURL   *string   `json:"poster_url"` // punter: null quan no hi ha pòster
	CreatedAt   time.Time `json:"created_at"`
}

// MovieDetail és la pel·lícula més dades CALCULADES a partir de les
// valoracions (fitxa i llistat del catàleg). No es guarden enlloc; es calculen
// a cada consulta.
type MovieDetail struct {
	Movie
	AvgScore    *float64 `json:"avg_score"` // null si no hi ha cap valoració
	RatingCount int      `json:"rating_count"`
}

// MovieInput és el cos que accepten POST i PUT. Separar-lo de Movie evita que
// el client pugui enviar camps que no li toquen (id, created_at, poster_url).
type MovieInput struct {
	Title       string   `json:"title"`
	Year        int      `json:"year"`
	DurationMin int      `json:"duration_min"`
	Director    string   `json:"director"`
	Synopsis    string   `json:"synopsis"`
	Genres      []string `json:"genres"`
	Status      string   `json:"status"`
}

// Normalize neteja espais sobrants i aplica valors per defecte.
func (in *MovieInput) Normalize() {
	in.Title = strings.TrimSpace(in.Title)
	in.Director = strings.TrimSpace(in.Director)
	in.Synopsis = strings.TrimSpace(in.Synopsis)
	if in.Status == "" {
		in.Status = StatusDraft
	}
}

// Validate retorna el primer error trobat, amb un missatge pensat per
// mostrar-se tal qual a l'usuari (el handler el converteix en un 400).
func (in MovieInput) Validate() error {
	switch {
	case in.Title == "":
		return errors.New("title és obligatori")
	case in.Year < 1888 || in.Year > time.Now().Year()+5:
		// 1888: la pel·lícula més antiga conservada. +5: permet estrenes anunciades.
		return fmt.Errorf("year ha d'estar entre 1888 i %d", time.Now().Year()+5)
	case in.DurationMin <= 0:
		return errors.New("duration_min ha de ser positiu")
	case in.Director == "":
		return errors.New("director és obligatori")
	case len(in.Genres) == 0:
		return errors.New("cal com a mínim un gènere")
	case in.Status != StatusDraft && in.Status != StatusPublished:
		return errors.New("status ha de ser 'draft' o 'published'")
	}
	for _, g := range in.Genres {
		if !slices.Contains(Genres, g) {
			return fmt.Errorf("gènere desconegut: %q", g)
		}
	}
	return nil
}
