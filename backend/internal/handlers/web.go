package handlers

import (
	"net/http"
	"os"
	"path"
	"path/filepath"
	"strings"
)

// Web serveix el build de Vue (web/dist) des de la mateixa API (§9, Opció A).
//
// Per què des de Go i no un servei a part? Una sola URL per a tot: el web
// crida /api i carrega /uploads del MATEIX origen, així que no cal CORS i les
// URL relatives dels pòsters funcionen tal qual (igual que amb el proxy de
// Vite en desenvolupament).
type Web struct {
	Dir     string // p. ex. /app/web (el contingut de web/dist)
	enabled bool
}

// NewWeb prepara el servei del web. Si a dir no hi ha cap build (en
// desenvolupament el web el serveix Vite a :5173), queda desactivat i les
// rutes desconegudes donen el 404 JSON de sempre.
func NewWeb(dir string) Web {
	if dir == "" {
		return Web{}
	}
	_, err := os.Stat(filepath.Join(dir, "index.html"))
	return Web{Dir: dir, enabled: err == nil}
}

// Enabled diu si hi ha un build del web per servir.
func (h Web) Enabled() bool { return h.enabled }

// Handler s'instal·la com a NotFound del router: només arriben aquí les
// peticions que no ha atès cap ruta de l'API.
//
//   - Fitxer que existeix (index.html, assets/app-3f2a.js, favicon.svg) → el fitxer.
//   - /api/…, /uploads/…, /health inexistents → 404 en JSON: un client de
//     l'API ha de rebre un error de l'API, no una pàgina HTML.
//   - Qualsevol altra ruta (/pelicula/123, /admin, /entra) → index.html. És
//     un SPA: qui entén aquestes rutes és el router de Vue, al navegador. Si
//     no fos així, recarregar /pelicula/123 donaria un 404.
func (h Web) Handler(w http.ResponseWriter, r *http.Request) {
	p := r.URL.Path
	if !h.enabled || strings.HasPrefix(p, "/api/") || p == "/api" || strings.HasPrefix(p, "/uploads/") || p == "/health" {
		writeError(w, http.StatusNotFound, "no trobat")
		return
	}
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		writeError(w, http.StatusMethodNotAllowed, "mètode no permès")
		return
	}

	// path.Clean amb una "/" al davant resol els "../": el resultat sempre
	// queda dins de Dir, per molt que algú demani /../../etc/passwd.
	clean := path.Clean("/" + p)
	file := filepath.Join(h.Dir, filepath.FromSlash(clean))
	if info, err := os.Stat(file); err == nil && !info.IsDir() {
		if strings.HasPrefix(clean, "/assets/") {
			// Vite posa un hash del contingut al nom (app-3f2a9c.js): si el
			// fitxer canvia, canvia el nom. Per tant, un nom concret no canvia
			// mai i es pot guardar a la cache un any sense preguntar.
			w.Header().Set("Cache-Control", "public, max-age=31536000, immutable")
		} else {
			w.Header().Set("Cache-Control", "no-cache")
		}
		http.ServeFile(w, r, file)
		return
	}

	// index.html NO es pot guardar a la cache sense preguntar: és el fitxer
	// que diu quins assets (amb quin hash) carregar. Si el navegador el
	// guardés, després d'un deploy continuaria demanant els JS vells.
	w.Header().Set("Cache-Control", "no-cache")
	http.ServeFile(w, r, filepath.Join(h.Dir, "index.html"))
}
