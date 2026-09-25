// Package storage és l'única capa que parla amb PostgreSQL. Els handlers no
// escriuen SQL: criden funcions d'aquí i reben structs de models.
package storage

import (
	"context"
	"errors"
	"fmt"

	"github.com/golang-migrate/migrate/v4"
	migratepgx "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	"github.com/golang-migrate/migrate/v4/source/iofs"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jackc/pgx/v5/stdlib"

	"github.com/jaumepape/cinecat/backend/migrations"
)

// ErrNotFound indica que el recurs demanat no existeix. El handler el
// tradueix a un 404; la capa de storage no sap res de codis HTTP.
var ErrNotFound = errors.New("no trobat")

// Connect obre un pool de connexions a partir de DATABASE_URL i comprova amb
// un Ping que la BD respon. Un pool reaprofita connexions entre peticions en
// lloc d'obrir-ne una de nova cada vegada (que és lent).
func Connect(ctx context.Context, databaseURL string) (*pgxpool.Pool, error) {
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, fmt.Errorf("configurant el pool: %w", err)
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("connectant a la BD: %w", err)
	}
	return pool, nil
}

// Migrate aplica les migracions pendents (les incrustades al binari).
// golang-migrate apunta a la taula schema_migrations quina versió s'ha
// aplicat, així que tornar-ho a executar no fa res si ja està al dia.
func Migrate(pool *pgxpool.Pool) error {
	src, err := iofs.New(migrations.FS, ".")
	if err != nil {
		return fmt.Errorf("llegint migracions: %w", err)
	}
	// golang-migrate treballa amb database/sql; OpenDBFromPool l'embolcalla
	// sobre el nostre pool, així no cal una segona cadena de connexió.
	db := stdlib.OpenDBFromPool(pool)
	defer db.Close()
	driver, err := migratepgx.WithInstance(db, &migratepgx.Config{})
	if err != nil {
		return fmt.Errorf("preparant el driver de migracions: %w", err)
	}
	m, err := migrate.NewWithInstance("iofs", src, "pgx5", driver)
	if err != nil {
		return fmt.Errorf("preparant les migracions: %w", err)
	}
	// El driver reté una connexió del pool mentre és obert. Si no el tanquem,
	// aquella connexió no torna mai al pool (i pool.Close() es queda esperant).
	defer m.Close()
	if err := m.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("aplicant migracions: %w", err)
	}
	return nil
}
