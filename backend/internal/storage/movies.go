package storage

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/jaumepape/cinecat/backend/internal/models"
)

// MovieStore agrupa les consultes SQL de la taula movies.
type MovieStore struct {
	pool *pgxpool.Pool
}

func NewMovieStore(pool *pgxpool.Pool) *MovieStore {
	return &MovieStore{pool: pool}
}

// movieColumns és la llista de columnes que llegim sempre en el mateix ordre,
// perquè scanMovie les pugui assignar. id::text: el client rep l'UUID com a
// text. Si hi ha poster_path, la URL pública és /uploads/<ruta> (Fase 2).
const movieColumns = `
	m.id::text, m.title, m.year, m.duration_min, m.director, m.synopsis,
	m.genres, m.status, '/uploads/' || m.poster_path, m.created_at`

// rowScanner és el que tenen en comú pgx.Row i pgx.Rows.
type rowScanner interface {
	Scan(dest ...any) error
}

func scanMovie(row rowScanner, extra ...any) (models.Movie, error) {
	var m models.Movie
	dest := append([]any{
		&m.ID, &m.Title, &m.Year, &m.DurationMin, &m.Director, &m.Synopsis,
		&m.Genres, &m.Status, &m.PosterURL, &m.CreatedAt,
	}, extra...)
	err := row.Scan(dest...)
	if errors.Is(err, pgx.ErrNoRows) {
		return m, ErrNotFound
	}
	// Tots els instants surten en UTC ("...Z"), sigui quina sigui la zona
	// del servidor: els clients ja els convertiran a l'hora local.
	m.CreatedAt = m.CreatedAt.UTC()
	return m, err
}

// statsColumns i statsJoin afegeixen a una consulta de movies la mitjana i el
// nombre de valoracions, CALCULATS ara mateix a partir de la taula ratings.
// Guardar-los a movies seria duplicar dades: caldria mantenir-los
// sincronitzats a cada valoració nova i, si mai divergissin, no sabríem quina
// versió és la bona.
//
// LEFT JOIN: una pel·lícula sense valoracions també ha de sortir. En aquest
// cas AVG retorna NULL (→ avg_score: null) i COUNT(r.id) retorna 0.
const (
	statsColumns = `,
		ROUND(AVG(r.score), 1)::float8,
		COUNT(r.id)`
	statsJoin = `
		LEFT JOIN ratings r ON r.movie_id = m.id`
)

func scanMovieDetail(row rowScanner) (models.MovieDetail, error) {
	var d models.MovieDetail
	m, err := scanMovie(row, &d.AvgScore, &d.RatingCount)
	d.Movie = m
	return d, err
}

// List retorna el catàleg amb la mitjana de cada pel·lícula (la targeta del
// catàleg la mostra). Tot en UNA consulta: un JOIN + GROUP BY, en lloc d'una
// consulta de mitjana per a cada pel·lícula (el problema "N+1").
//
// Ordre alfabètic amb la col·lació ICU: entén els accents ("Òrbita" va amb
// les O, no darrere la Z com passaria ordenant per bytes).
//
// q, genre i status són opcionals: una cadena buida vol dir "no filtris".
// Tenir una sola consulta amb condicions opcionals és més llegible que
// construir el SQL concatenant trossos segons els filtres. Els valors van
// sempre com a paràmetres ($1, $2...), mai enganxats al text del SQL: així és
// impossible una injecció SQL.
func (s *MovieStore) List(ctx context.Context, q, genre, status string) ([]models.MovieDetail, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT `+movieColumns+statsColumns+`
		FROM movies m`+statsJoin+`
		WHERE ($1 = '' OR m.title ILIKE '%' || $1 || '%')
		  AND ($2 = '' OR $2 = ANY(m.genres))
		  AND ($3 = '' OR m.status = $3)
		GROUP BY m.id
		ORDER BY m.title COLLATE "und-x-icu"`, q, genre, status)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	movies := []models.MovieDetail{} // mai nil: el JSON serà [] i no null
	for rows.Next() {
		d, err := scanMovieDetail(rows)
		if err != nil {
			return nil, err
		}
		movies = append(movies, d)
	}
	return movies, rows.Err()
}

// Get retorna la fitxa d'una pel·lícula amb la mitjana i el recompte.
func (s *MovieStore) Get(ctx context.Context, id string) (models.MovieDetail, error) {
	row := s.pool.QueryRow(ctx, `
		SELECT `+movieColumns+statsColumns+`
		FROM movies m`+statsJoin+`
		WHERE m.id = $1
		GROUP BY m.id`, id)
	return scanMovieDetail(row)
}

// Create insereix una pel·lícula. RETURNING retorna la fila tal com ha quedat
// (amb l'id i el created_at que ha generat Postgres) en la mateixa consulta.
func (s *MovieStore) Create(ctx context.Context, in models.MovieInput) (models.Movie, error) {
	row := s.pool.QueryRow(ctx, `
		INSERT INTO movies AS m (title, year, duration_min, director, synopsis, genres, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING `+movieColumns,
		in.Title, in.Year, in.DurationMin, in.Director, in.Synopsis, in.Genres, in.Status)
	return scanMovie(row)
}

// Update substitueix totes les metadades (semàntica de PUT). Si cap fila
// coincideix amb l'id, RETURNING no retorna res i scanMovie dona ErrNotFound.
func (s *MovieStore) Update(ctx context.Context, id string, in models.MovieInput) (models.Movie, error) {
	row := s.pool.QueryRow(ctx, `
		UPDATE movies AS m
		SET title = $2, year = $3, duration_min = $4, director = $5,
		    synopsis = $6, genres = $7, status = $8
		WHERE m.id = $1
		RETURNING `+movieColumns,
		id, in.Title, in.Year, in.DurationMin, in.Director, in.Synopsis, in.Genres, in.Status)
	return scanMovie(row)
}

// SetPosterPath apunta la pel·lícula al seu fitxer de pòster.
func (s *MovieStore) SetPosterPath(ctx context.Context, id, path string) error {
	tag, err := s.pool.Exec(ctx, `UPDATE movies SET poster_path = $2 WHERE id = $1`, id, path)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

// Exists diu si hi ha una pel·lícula amb aquest id.
func (s *MovieStore) Exists(ctx context.Context, id string) (bool, error) {
	var exists bool
	err := s.pool.QueryRow(ctx, `SELECT EXISTS (SELECT 1 FROM movies WHERE id = $1)`, id).Scan(&exists)
	return exists, err
}

// Delete esborra la pel·lícula (i, per ON DELETE CASCADE, les seves valoracions).
func (s *MovieStore) Delete(ctx context.Context, id string) error {
	tag, err := s.pool.Exec(ctx, `DELETE FROM movies WHERE id = $1`, id)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}
