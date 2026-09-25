// Comanda createadmin: crea el primer usuari administrador (o en promociona
// un d'existent). Registrar-se per l'API sempre dona rol 'user'; ser admin
// només es pot aconseguir amb accés directe a la BD, com aquí.
//
// Ús:
//
//	DATABASE_URL=... go run ./cmd/createadmin -email admin@exemple.cat -alias admin
//
// La contrasenya NO es passa com a argument: quedaria a l'historial de la
// shell i visible a `ps`. Es llegeix de la variable CINECAT_ADMIN_PASSWORD o,
// si no hi és, es demana pel terminal sense mostrar-la.
package main

import (
	"bufio"
	"context"
	"errors"
	"flag"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	"golang.org/x/term"

	"github.com/jaumepape/cinecat/backend/internal/auth"
	"github.com/jaumepape/cinecat/backend/internal/models"
	"github.com/jaumepape/cinecat/backend/internal/storage"
)

func main() {
	email := flag.String("email", "", "email de l'admin (obligatori)")
	alias := flag.String("alias", "", "àlies públic (obligatori si l'usuari encara no existeix)")
	flag.Parse()

	databaseURL := os.Getenv("DATABASE_URL")
	if databaseURL == "" {
		log.Fatal("falta la variable d'entorn DATABASE_URL")
	}
	in := models.RegisterInput{Email: *email, Alias: *alias}
	in.Normalize()
	if err := models.ValidateEmail(in.Email); err != nil {
		log.Fatalf("-email: %v", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	pool, err := storage.Connect(ctx, databaseURL)
	if err != nil {
		log.Fatalf("base de dades: %v", err)
	}
	defer pool.Close()
	// Per si la BD és nova i el servidor encara no hi ha arrencat mai.
	if err := storage.Migrate(pool); err != nil {
		log.Fatalf("migracions: %v", err)
	}
	users := storage.NewUserStore(pool)

	// Si l'usuari ja existeix (p. ex. s'ha registrat pel web), el promocionem
	// i no toquem la seva contrasenya.
	if u, err := users.SetRole(ctx, in.Email, auth.RoleAdmin); err == nil {
		fmt.Printf("L'usuari existent @%s (%s) ara és admin.\n", u.Alias, u.Email)
		return
	} else if !errors.Is(err, storage.ErrNotFound) {
		log.Fatal(err)
	}

	password, err := readPassword()
	if err != nil {
		log.Fatal(err)
	}
	in.Password = password
	if err := in.Validate(); err != nil {
		log.Fatal(err)
	}
	hash, err := auth.HashPassword(in.Password)
	if err != nil {
		log.Fatal(err)
	}
	u, err := users.Create(ctx, in.Email, in.Alias, hash, auth.RoleAdmin)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("Admin creat: @%s (%s).\n", u.Alias, u.Email)
}

// readPassword llegeix la contrasenya de CINECAT_ADMIN_PASSWORD, del terminal
// sense eco, o de stdin si no és un terminal (p. ex. amb una canonada).
func readPassword() (string, error) {
	if p := os.Getenv("CINECAT_ADMIN_PASSWORD"); p != "" {
		return p, nil
	}
	fd := int(os.Stdin.Fd())
	if term.IsTerminal(fd) {
		fmt.Fprint(os.Stderr, "Contrasenya de l'admin: ")
		p, err := term.ReadPassword(fd)
		fmt.Fprintln(os.Stderr)
		return string(p), err
	}
	line, err := bufio.NewReader(os.Stdin).ReadString('\n')
	if err != nil && line == "" {
		return "", errors.New("no s'ha pogut llegir la contrasenya de stdin")
	}
	return strings.TrimRight(line, "\r\n"), nil
}
