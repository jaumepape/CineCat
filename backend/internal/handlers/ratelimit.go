package handlers

import (
	"math"
	"net"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"golang.org/x/time/rate"
)

// RateLimiter limita quantes peticions pot fer cada IP. És la "mitigació
// mínima d'abús" de §5 per a les valoracions anònimes: no impedeix que algú
// voti dues vegades, però sí que un script n'enviï milers.
//
// Cada IP té un "cubell de fitxes" (token bucket): hi caben Burst fitxes, cada
// petició en gasta una i se'n recupera una cada Every. Així es permeten unes
// quantes valoracions seguides, però no un raig continu.
//
// Limitació conscient: l'estat viu a la memòria del procés. Es perd en
// reiniciar i no es comparteix entre rèpliques. Amb una sola instància (el
// nostre cas) és suficient; amb N caldria un magatzem compartit (p. ex. Redis).
type RateLimiter struct {
	every      time.Duration
	burst      int
	trustProxy bool

	mu      sync.Mutex
	clients map[string]*client
}

type client struct {
	limiter  *rate.Limiter
	lastSeen time.Time
}

// NewRateLimiter crea el limitador i arrenca una neteja periòdica perquè el
// mapa d'IPs no creixi indefinidament.
//
// trustProxy: si és true, la IP es llegeix de X-Forwarded-For (vegeu clientIP).
func NewRateLimiter(every time.Duration, burst int, trustProxy bool) *RateLimiter {
	rl := &RateLimiter{every: every, burst: burst, trustProxy: trustProxy, clients: map[string]*client{}}
	go rl.cleanup()
	return rl
}

// Middleware respon 429 quan una IP supera el límit. S'aplica només a les
// rutes que ho necessiten (r.With(...)), no a tota l'API.
func (rl *RateLimiter) Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if !rl.allow(clientIP(r, rl.trustProxy)) {
			// Retry-After diu al client quants segons ha d'esperar.
			w.Header().Set("Retry-After", strconv.Itoa(int(math.Ceil(rl.every.Seconds()))))
			writeError(w, http.StatusTooManyRequests, "massa peticions seguides; espera una mica")
			return
		}
		next.ServeHTTP(w, r)
	})
}

func (rl *RateLimiter) allow(ip string) bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()
	c, ok := rl.clients[ip]
	if !ok {
		c = &client{limiter: rate.NewLimiter(rate.Every(rl.every), rl.burst)}
		rl.clients[ip] = c
	}
	c.lastSeen = time.Now()
	return c.limiter.Allow()
}

// cleanup esborra les IPs inactives. Quan una IP porta prou temps sense
// venir, el seu cubell ja s'ha tornat a omplir: esborrar-la no canvia res.
func (rl *RateLimiter) cleanup() {
	idle := rl.every * time.Duration(rl.burst)
	for range time.Tick(time.Minute) {
		rl.mu.Lock()
		for ip, c := range rl.clients {
			if time.Since(c.lastSeen) > idle {
				delete(rl.clients, ip)
			}
		}
		rl.mu.Unlock()
	}
}

// clientIP obté la IP del visitant.
//
// Sense proxy, és l'adreça de la connexió TCP (RemoteAddr): no es pot falsificar.
//
// A Railway, però, entre el visitant i nosaltres hi ha el proxy de Railway, i
// RemoteAddr és sempre la IP del proxy: tothom compartiria el mateix límit. El
// proxy afegeix la IP real al final de la capçalera X-Forwarded-For. Agafem
// l'ÚLTIMA entrada, la que ha escrit el proxy: les anteriors les pot haver
// posat el client i són falsificables. Per això només ens en fiem si
// TRUST_PROXY està activat (és a dir, si sabem que hi ha un proxy davant).
func clientIP(r *http.Request, trustProxy bool) string {
	if trustProxy {
		if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
			parts := strings.Split(xff, ",")
			return strings.TrimSpace(parts[len(parts)-1])
		}
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}
