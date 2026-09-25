// Comando server: punt d'entrada de l'API de CineCat.
//
// Aquí només es "munta" l'aplicació: es llegeix la configuració de l'entorn,
// s'obre la BD, s'apliquen les migracions i es connecten les rutes amb els
// handlers. La lògica viu a internal/.
package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/jaumepape/cinecat/backend/internal/handlers"
	"github.com/jaumepape/cinecat/backend/internal/storage"
)

func main() {
	// Railway injecta PORT; en local fem servir 8080 per defecte.
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	// DATABASE_URL tampoc es fixa mai al codi: a Railway és una variable
	// referenciada al plugin Postgres; en local, una variable d'entorn.
	databaseURL := os.Getenv("DATABASE_URL")
	if databaseURL == "" {
		log.Fatal("falta la variable d'entorn DATABASE_URL")
	}

	// Si la BD no respon en 10 s a l'arrencada, és millor fallar de seguida
	// (Railway reintentarà el desplegament) que quedar-se penjat.
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	pool, err := storage.Connect(ctx, databaseURL)
	if err != nil {
		log.Fatalf("base de dades: %v", err)
	}
	defer pool.Close()

	// Migracions en arrencar: el binari porta els SQL incrustats i els aplica
	// abans d'acceptar peticions. És el camí més simple (un sol desplegament,
	// codi i esquema sempre alineats). Amb diverses rèpliques o migracions
	// llargues convindria un pas separat, però aquí tenim una sola instància.
	if err := storage.Migrate(pool); err != nil {
		log.Fatalf("migracions: %v", err)
	}
	log.Print("migracions al dia")

	r := chi.NewRouter()
	// Middlewares: un log per petició i recuperació de panics (un panic en un
	// handler retorna 500 en lloc de tombar tot el servidor).
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	r.Get("/health", handlers.Health)
	r.Route("/api/movies", handlers.Movies{Store: storage.NewMovieStore(pool)}.Routes)

	addr := ":" + port
	log.Printf("CineCat backend escoltant a %s", addr)
	if err := http.ListenAndServe(addr, r); err != nil {
		log.Fatalf("el servidor s'ha aturat: %v", err)
	}
}
