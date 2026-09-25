package storage

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/jaumepape/cinecat/backend/internal/models"
)

// RatingStore agrupa les consultes SQL de la taula ratings.
type RatingStore struct {
	pool *pgxpool.Pool
}

func NewRatingStore(pool *pgxpool.Pool) *RatingStore {
	return &RatingStore{pool: pool}
}

const ratingColumns = `
	id::text, movie_id::text, user_id::text, score, comment, author_label, created_at`

func scanRating(row rowScanner) (models.Rating, error) {
	var r models.Rating
	err := row.Scan(&r.ID, &r.MovieID, &r.UserID, &r.Score, &r.Comment, &r.AuthorLabel, &r.CreatedAt)
	r.CreatedAt = r.CreatedAt.UTC()
	return r, err
}

// List retorna una pàgina de valoracions d'una pel·lícula, les més recents
// primer. La paginació és per OFFSET: senzilla d'entendre i suficient per a
// uns quants centenars de valoracions. (Amb milions, OFFSET es torna lent i es
// pagina "per cursor": WHERE created_at < l'última vista.)
//
// L'id al final de l'ORDER BY desempata dues valoracions amb el mateix
// created_at; sense ell, l'ordre entre pàgines no seria estable i una
// valoració podria sortir repetida o no sortir.
func (s *RatingStore) List(ctx context.Context, movieID string, page, pageSize int) ([]models.Rating, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT `+ratingColumns+`
		FROM ratings
		WHERE movie_id = $1
		ORDER BY created_at DESC, id DESC
		LIMIT $2 OFFSET $3`, movieID, pageSize, (page-1)*pageSize)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	ratings := []models.Rating{}
	for rows.Next() {
		r, err := scanRating(rows)
		if err != nil {
			return nil, err
		}
		ratings = append(ratings, r)
	}
	return ratings, rows.Err()
}

// Create insereix una valoració. userID és nil per a una valoració anònima;
// a la Fase 4 el handler hi posarà l'id de l'usuari si arriba un token.
//
// No cal comprovar abans que la pel·lícula existeixi: la clau forana ho fa per
// nosaltres. Si movie_id no existeix, Postgres rebutja l'INSERT amb l'error
// 23503 (foreign_key_violation) i el traduïm a ErrNotFound.
func (s *RatingStore) Create(ctx context.Context, movieID string, userID *string, in models.RatingInput) (models.Rating, error) {
	row := s.pool.QueryRow(ctx, `
		INSERT INTO ratings (movie_id, user_id, score, comment, author_label)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING `+ratingColumns,
		movieID, userID, in.Score, in.Comment, in.AuthorLabel)
	r, err := scanRating(row)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23503" {
		return r, ErrNotFound
	}
	if errors.Is(err, pgx.ErrNoRows) {
		return r, ErrNotFound
	}
	return r, err
}
