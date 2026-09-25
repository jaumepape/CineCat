import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// En desenvolupament, Vite serveix el web a :5173 i l'API corre a :8080.
// El proxy fa que les peticions a /api i /uploads que arriben a Vite es
// reenviïn a l'API. Per al navegador tot ve del MATEIX origen (localhost:5173):
//  - no cal configurar CORS a l'API;
//  - poster_url és relativa ("/uploads/posters/<id>.jpg") i funciona tal qual.
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
})
