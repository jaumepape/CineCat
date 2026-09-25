import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router.js'
import { useAuthStore } from './stores/auth.js'
import './styles/tokens.css'
import './styles/base.css'

const app = createApp(App).use(createPinia())
// La sessió s'ha de recuperar ABANS que el router decideixi res: si no, en
// recarregar /admin, el guard creuria que no hi ha sessió.
useAuthStore().restore()
app.use(router).mount('#app')
