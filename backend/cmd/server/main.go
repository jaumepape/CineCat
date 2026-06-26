// Comando server: punt d'entrada de l'API de CineCat.
//
// A la Fase 0 només aixeca un servidor HTTP mínim amb un endpoint
// GET /health. L'objectiu d'aquesta fase és tenir el "tub" sencer
// (codi -> Railway -> URL pública) funcionant abans d'afegir-hi domini:
// base de dades, catàleg, imatges i auth arribaran a fases posteriors.
package main

import (
	"log"
	"net/http"
	"os"
)

func main() {
	// Railway (i molts PaaS) injecten el port d'escolta per la variable
	// d'entorn PORT. El nostre codi NO ha de fixar un port: l'ha de llegir
	// de l'entorn. En local, si no hi ha PORT, fem servir un valor per
	// defecte raonable perquè `docker run`/desenvolupament sigui còmode.
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	// ServeMux és el router de la llibreria estàndard. Per a un sol
	// endpoint no cal cap dependència externa; el router `chi` (decidit a
	// l'especificació §6) entrarà a la Fase 1, quan hi hagi rutes reals.
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", healthHandler)

	addr := ":" + port
	log.Printf("CineCat backend escoltant a %s", addr)

	// ListenAndServe bloqueja fins que el servidor s'atura. Si retorna un
	// error (p. ex. el port ja està ocupat), el registrem i sortim.
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("el servidor s'ha aturat: %v", err)
	}
}

// healthHandler respon 200 OK. És el "batec" que Railway i nosaltres
// fem servir per comprovar que el servei és viu. Retorna un JSON mínim
// perquè sigui llegible tant per humans com per eines.
func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(`{"status":"ok"}`))
}
