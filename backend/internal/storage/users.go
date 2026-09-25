package storage

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/jaumepape/cinecat/backend/internal/models"
)

// ErrEmailTaken i ErrAliasTaken: violació de la restricció UNIQUE de la
// columna corresponent. El handler les converteix en un 409 Conflict.
var (
	ErrEmailTaken = errors.New("aquest email ja està registrat")
	ErrAliasTaken = errors.New("aquest àlies ja està agafat")
)

// UserStore agrupa les consultes SQL de la taula users.
type UserStore struct {
	pool *pgxpool.Pool
}

func NewUserStore(pool *pgxpool.Pool) *UserStore {
	return &UserStore{pool: pool}
}

const userColumns = `id::text, email, alias, role, created_at`

func scanUser(row rowScanner, extra ...any) (models.User, error) {
	var u models.User
	err := row.Scan(append([]any{&u.ID, &u.Email, &u.Alias, &u.Role, &u.CreatedAt}, extra...)...)
	if errors.Is(err, pgx.ErrNoRows) {
		return u, ErrNotFound
	}
	u.CreatedAt = u.CreatedAt.UTC()
	return u, err
}

// Create insereix un usuari. No comprovem abans si l'email existeix (entre
// la comprovació i l'INSERT un altre podria registrar-lo): deixem que ho
// garanteixi la restricció UNIQUE de la BD i traduïm l'error.
func (s *UserStore) Create(ctx context.Context, email, alias, passwordHash, role string) (models.User, error) {
	row := s.pool.QueryRow(ctx, `
		INSERT INTO users (email, alias, password_hash, role)
		VALUES ($1, $2, $3, $4)
		RETURNING `+userColumns, email, alias, passwordHash, role)
	u, err := scanUser(row)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23505" { // unique_violation
		switch pgErr.ConstraintName {
		case "users_email_key":
			return u, ErrEmailTaken
		case "users_alias_key":
			return u, ErrAliasTaken
		}
	}
	return u, err
}

// GetByEmail retorna l'usuari i el seu hash (només per al login).
func (s *UserStore) GetByEmail(ctx context.Context, email string) (models.User, string, error) {
	var hash string
	row := s.pool.QueryRow(ctx, `SELECT `+userColumns+`, password_hash FROM users WHERE email = $1`, email)
	u, err := scanUser(row, &hash)
	return u, hash, err
}

// SetRole canvia el rol d'un usuari existent (el fa servir createadmin).
func (s *UserStore) SetRole(ctx context.Context, email, role string) (models.User, error) {
	row := s.pool.QueryRow(ctx, `UPDATE users SET role = $2 WHERE email = $1 RETURNING `+userColumns, email, role)
	return scanUser(row)
}
