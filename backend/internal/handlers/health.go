package handlers

import "net/http"

// Health respon 200 OK. És el "batec" que Railway fa servir per saber que el
// servei és viu (healthcheckPath a railway.json).
func Health(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}
