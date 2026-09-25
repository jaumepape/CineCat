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
	"strconv"
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

	// UPLOAD_DIR: on es desen els pòsters. En local, "uploads" (relatiu a on
	// s'executa: backend/uploads amb `go run`); a Railway, el volum persistent
	// muntat a /app/uploads. El disc d'un contenidor s'esborra a cada deploy;
	// el volum no.
	uploadDir := os.Getenv("UPLOAD_DIR")
	if uploadDir == "" {
		uploadDir = "uploads"
	}
	maxUploadMB := 5
	if v := os.Getenv("MAX_UPLOAD_MB"); v != "" {
		n, err := strconv.Atoi(v)
		if err != nil || n <= 0 {
			log.Fatalf("MAX_UPLOAD_MB ha de ser un enter positiu, no %q", v)
		}
		maxUploadMB = n
	}
	// TRUST_PROXY=true quan hi ha un proxy davant (Railway): llavors la IP del
	// visitant es llegeix de X-Forwarded-For. En local, fals: la IP és la de
	// la connexió i ningú la pot falsificar amb una capçalera.
	trustProxy := os.Getenv("TRUST_PROXY") == "true"
	posterFiles := storage.PosterFiles{Dir: uploadDir}
	if err := posterFiles.Init(); err != nil {
		log.Fatalf("preparant UPLOAD_DIR: %v", err)
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
	// HEAD (només capçaleres) respon com el GET corresponent, sense cos. Ho fan
	// servir navegadors i eines per mirar mida o data d'un fitxer sense baixar-lo.
	r.Use(middleware.GetHead)

	movieStore := storage.NewMovieStore(pool)
	posters := handlers.Posters{
		Store:    movieStore,
		Files:    posterFiles,
		MaxBytes: int64(maxUploadMB) << 20, // MB → bytes
	}

	ratings := handlers.Ratings{Movies: movieStore, Store: storage.NewRatingStore(pool)}
	// Valoracions anònimes: fins a 5 seguides per IP i, després, 1 cada 12 s
	// (≈ 5 per minut). Molt per sobre d'una persona real, molt per sota d'un script.
	ratingLimiter := handlers.NewRateLimiter(12*time.Second, 5, trustProxy)

	r.Get("/health", handlers.Health)
	r.Route("/api/movies", func(r chi.Router) {
		handlers.Movies{Store: movieStore, Posters: posters}.Routes(r)
		r.Get("/{id}/ratings", ratings.List)
		// With aplica el middleware NOMÉS a aquesta ruta.
		r.With(ratingLimiter.Middleware).Post("/{id}/ratings", ratings.Create)
	})
	// Pujar (POST .../poster, JSON + multipart) i servir (GET /uploads/...,
	// bytes) són dos endpoints diferents: el primer escriu, el segon és un
	// fitxer estàtic públic i cacheable.
	r.Get("/uploads/posters/{file}", posters.Serve)

	addr := ":" + port
	log.Printf("CineCat backend escoltant a %s (UPLOAD_DIR=%s, MAX_UPLOAD_MB=%d)", addr, uploadDir, maxUploadMB)
	if err := http.ListenAndServe(addr, r); err != nil {
		log.Fatalf("el servidor s'ha aturat: %v", err)
	}
}
