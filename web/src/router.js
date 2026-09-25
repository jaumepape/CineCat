import { createRouter, createWebHistory } from 'vue-router'
import CatalogView from './views/CatalogView.vue'
import { useAuthStore } from './stores/auth.js'

// Rutes del web. Les vistes que no són el catàleg es carreguen "a demanda"
// (import dinàmic): Vite en fa fitxers JS a part que només es baixen quan cal.
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'catalog', component: CatalogView, meta: { title: 'CineCat' } },
    {
      path: '/pelicula/:id',
      name: 'movie',
      component: () => import('./views/MovieView.vue'),
      props: true, // l':id arriba a la vista com a prop
    },
    {
      path: '/entra',
      name: 'login',
      component: () => import('./views/LoginView.vue'),
      meta: { title: 'Inicia sessió · CineCat', guestOnly: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('./views/AdminListView.vue'),
      meta: { title: 'Gestió del catàleg · CineCat', requiresAdmin: true },
    },
    {
      path: '/admin/pelicules/nova',
      name: 'admin-new',
      component: () => import('./views/AdminFormView.vue'),
      meta: { title: 'Nova pel·lícula · CineCat', requiresAdmin: true },
    },
    {
      path: '/admin/pelicules/:id',
      name: 'admin-edit',
      component: () => import('./views/AdminFormView.vue'),
      props: true,
      meta: { title: 'Edita la pel·lícula · CineCat', requiresAdmin: true },
    },
    { path: '/:pathMatch(.*)*', component: () => import('./views/NotFoundView.vue'), meta: { title: 'No trobat · CineCat' } },
  ],
  // En tornar enrere, recupera la posició de scroll; si no, amunt de tot.
  // Canviar només els filtres (la query) no ha de fer saltar la pàgina.
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.path === from.path) return false
    return { top: 0 }
  },
})

// Guard de rutes. IMPORTANT: això és només comoditat d'interfície (no mostrar
// pantalles que no pots fer servir). La seguretat de debò és a l'API: encara
// que algú se saltés aquest guard, qualsevol petició d'admin sense token
// d'admin rebria un 401/403.
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    // Amb sessió però sense ser admin: no té sentit enviar-lo a login.
    if (auth.isLoggedIn) return { name: 'catalog' }
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) return { name: 'catalog' }
})

router.afterEach((to) => {
  if (to.meta.title) document.title = to.meta.title
})
