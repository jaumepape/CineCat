<script setup>
// Vista 03 · Inicia sessió / Registre. Iniciar sessió és OPCIONAL: tot el
// catàleg es pot fer servir i valorar sense compte.
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({ alias: '', email: '', password: '' })
const loginError = ref(null)
const registerError = ref(null)
const sending = ref(null) // 'login' | 'register' | null

// Després d'entrar, tornem on era l'usuari (p. ex. la fitxa que volia valorar
// o la pàgina d'admin que ha demanat). Només acceptem rutes internes ("/...")
// perquè un enllaç maliciós no ens pugui enviar a una altra web.
function goBack() {
  const redirect = route.query.redirect
  const safe = typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
  router.replace(safe ? redirect : auth.isAdmin ? '/admin' : '/')
}

async function submitLogin() {
  sending.value = 'login'
  loginError.value = null
  try {
    await auth.login({ ...loginForm })
    goBack()
  } catch (err) {
    loginError.value = err.message
  } finally {
    sending.value = null
  }
}

async function submitRegister() {
  sending.value = 'register'
  registerError.value = null
  try {
    await auth.register({ ...registerForm })
    goBack()
  } catch (err) {
    registerError.value = err.message
  } finally {
    sending.value = null
  }
}
</script>

<template>
  <div class="page login">
    <p class="banner">
      <strong>Iniciar sessió és opcional.</strong>
      Pots valorar de manera anònima. Amb un compte, les teves valoracions porten el teu àlies i les pots editar.
    </p>

    <div class="cards">
      <form class="card" novalidate @submit.prevent="submitLogin">
        <h1>Inicia sessió</h1>
        <label class="field">
          <span class="field-label">Correu electrònic</span>
          <input v-model="loginForm.email" class="input" type="email" autocomplete="email" required />
        </label>
        <label class="field">
          <span class="field-label">Contrasenya</span>
          <input v-model="loginForm.password" class="input" type="password" autocomplete="current-password" required />
        </label>
        <span class="forgot" title="Properament">Has oblidat la contrasenya?</span>
        <p v-if="loginError" class="error" role="alert">{{ loginError }}</p>
        <button class="btn btn-primary" type="submit" :disabled="sending !== null">
          {{ sending === 'login' ? 'Entrant…' : 'Entra' }}
        </button>
      </form>

      <form class="card" novalidate @submit.prevent="submitRegister">
        <h2>Crea un compte</h2>
        <label class="field">
          <span class="field-label">Àlies públic</span>
          <span class="alias-input">
            <span class="at" aria-hidden="true">@</span>
            <input
              v-model="registerForm.alias"
              class="input"
              autocomplete="username"
              placeholder="joancinema"
              maxlength="20"
              required
            />
          </span>
          <span class="hint">És el nom que es veurà a les teves valoracions. De 3 a 20 caràcters: a-z, 0-9 o _.</span>
        </label>
        <label class="field">
          <span class="field-label">Correu electrònic</span>
          <input v-model="registerForm.email" class="input" type="email" autocomplete="email" required />
        </label>
        <label class="field">
          <span class="field-label">Contrasenya (mínim 8 caràcters)</span>
          <input v-model="registerForm.password" class="input" type="password" autocomplete="new-password" minlength="8" required />
        </label>
        <p v-if="registerError" class="error" role="alert">{{ registerError }}</p>
        <button class="btn btn-ghost accent" type="submit" :disabled="sending !== null">
          {{ sending === 'register' ? 'Creant…' : 'Crea el compte' }}
        </button>
      </form>
    </div>

    <RouterLink to="/" class="guest">o bé continua com a visitant →</RouterLink>
  </div>
</template>

<style scoped>
.login {
  max-width: 1160px;
  padding-top: 28px;
  padding-bottom: 60px;
}
.banner {
  margin: 0 0 24px;
  padding: 14px 18px;
  border-radius: var(--r-panel);
  background: var(--accent-12);
  border: 1px solid var(--accent-30);
  color: var(--text-2);
}
.banner strong {
  color: var(--accent);
}
.cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 26px;
  background: var(--surface);
  border: 1px solid var(--border-faint);
  border-radius: 14px;
}
h1,
h2 {
  margin: 0 0 4px;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.field {
  display: block;
}
.alias-input {
  position: relative;
  display: block;
}
.alias-input .input {
  padding-left: 30px;
}
.at {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-faint);
  font-family: var(--font-mono);
}
.hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-faint);
}
.forgot {
  align-self: flex-start;
  margin-top: -6px;
  font-size: 12.5px;
  color: var(--text-dim);
  cursor: default;
}
.btn {
  align-self: stretch;
}
.btn-ghost.accent {
  border-color: rgba(45, 212, 191, 0.4);
}
.error {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid var(--error);
  border-radius: var(--r-control);
  background: var(--error-bg);
  color: var(--error-text);
  font-size: 13px;
}
.guest {
  display: block;
  margin-top: 22px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13.5px;
}
.guest:hover {
  color: var(--accent);
}
@media (max-width: 760px) {
  .cards {
    grid-template-columns: 1fr;
  }
  .card {
    padding: 20px;
  }
}
</style>
