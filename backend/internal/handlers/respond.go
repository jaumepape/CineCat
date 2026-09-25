// Package handlers conté una funció per endpoint. Cada handler fa sempre el
// mateix recorregut: llegir la petició → validar → cridar storage → escriure
// JSON. No escriu SQL (això és feina de storage).
package handlers

import (
	"encoding/json"
	"log"
	"net/http"
)

// writeJSON escriu qualsevol valor com a JSON amb el codi d'estat indicat.
func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		log.Printf("escrivint la resposta JSON: %v", err)
	}
}

// writeError escriu el format d'error únic de l'API: {"error": "..."} (§4).
func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}

// serverError registra el detall de l'error als logs però al client només li
// diu "error del servidor": els detalls interns (SQL, rutes...) no s'exposen.
func serverError(w http.ResponseWriter, err error) {
	log.Printf("error intern: %v", err)
	writeError(w, http.StatusInternalServerError, "error del servidor")
}
