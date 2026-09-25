import { createRouter, createWebHistory } from 'vue-router'
import CatalogView from './views/CatalogView.vue'

// Rutes del web públic. La fitxa es carrega "a demanda" (import dinàmic):
// Vite en fa un fitxer JS a part que només es baixa quan cal.
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

router.afterEach((to) => {
  if (to.meta.title) document.title = to.meta.title
})
