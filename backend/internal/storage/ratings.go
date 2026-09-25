package storage

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/jaumepape/cinecat/backend/internal/models"
)

// ErrAlreadyRated: l'usuari ja té una valoració d'aquesta pel·lícula (índex
// únic ratings_movie_user_uniq). El handler el converteix en un 409.
var ErrAlreadyRated = errors.New("ja has valorat aquesta pel·lícula; pots editar la teva valoració")

// RatingStore agrupa les consultes SQL de la taula ratings.
type RatingStore struct {
	pool *pgxpool.Pool
}

func NewRatingStore(pool *pgxpool.Pool) *RatingStore {
	return &RatingStore{pool: pool}
}

// ratingSelect llegeix valoracions amb l'àlies de l'autor. LEFT JOIN: les
// anònimes (user_id NULL) no tenen usuari i han de sortir igualment, amb
// u.alias a NULL.
const ratingSelect = `
	SELECT r.id::text, r.movie_id::text, r.user_id::text, u.alias,
	       r.score, r.comment, r.author_label, r.created_at
	FROM ratings r
	LEFT JOIN users u ON u.id = r.user_id`

func scanRating(row rowScanner) (models.Rating, error) {
	var r models.Rating
	err := row.Scan(&r.ID, &r.MovieID, &r.UserID, &r.UserAlias, &r.Score, &r.Comment, &r.AuthorLabel, &r.CreatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return r, ErrNotFound
	}
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
	rows, err := s.pool.Query(ctx, ratingSelect+`
		WHERE r.movie_id = $1
		ORDER BY r.created_at DESC, r.id DESC
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

// Get retorna una valoració (per comprovar-ne l'autor abans d'editar-la).
func (s *RatingStore) Get(ctx context.Context, id string) (models.Rating, error) {
	return scanRating(s.pool.QueryRow(ctx, ratingSelect+` WHERE r.id = $1`, id))
}

// Create insereix una valoració. userID és nil per a una valoració anònima i
// l'id de l'usuari si n'hi ha un d'identificat: mateixa consulta per a tots
// dos casos.
//
// No cal comprovar abans que la pel·lícula existeixi: la clau forana ho fa per
// nosaltres. Si movie_id no existeix, Postgres rebutja l'INSERT amb l'error
// 23503 (foreign_key_violation) i el traduïm a ErrNotFound. Igualment, si
// l'usuari ja l'havia valorat, l'índex únic dona 23505.
func (s *RatingStore) Create(ctx context.Context, movieID string, userID *string, in models.RatingInput) (models.Rating, error) {
	var id string
	err := s.pool.QueryRow(ctx, `
		INSERT INTO ratings (movie_id, user_id, score, comment, author_label)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id::text`,
		movieID, userID, in.Score, in.Comment, in.AuthorLabel).Scan(&id)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) {
		switch pgErr.Code {
		case "23503": // foreign_key_violation
			return models.Rating{}, ErrNotFound
		case "23505": // unique_violation
			return models.Rating{}, ErrAlreadyRated
		}
	}
	if err != nil {
		return models.Rating{}, err
	}
	// Tornem a llegir-la amb el JOIN per retornar també l'àlies.
	return s.Get(ctx, id)
}

// Update canvia la nota i el comentari d'una valoració.
func (s *RatingStore) Update(ctx context.Context, id string, in models.RatingInput) (models.Rating, error) {
	tag, err := s.pool.Exec(ctx, `UPDATE ratings SET score = $2, comment = $3 WHERE id = $1`, id, in.Score, in.Comment)
	if err != nil {
		return models.Rating{}, err
	}
	if tag.RowsAffected() == 0 {
		return models.Rating{}, ErrNotFound
	}
	return s.Get(ctx, id)
}
