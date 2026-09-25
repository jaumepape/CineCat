<script setup>
// Capçalera fixa de 62px. Sense sessió: "Inicia sessió". Amb sessió: xip amb
// l'àlies i "Surt"; si és admin, badge ADMIN i enllaç a la gestió.
// "Novetats" i "Top valorades" són al disseny però fora de l'MVP.
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

function logout() {
  auth.logout()
  // Si era en una pàgina d'admin, ja no hi pot ser.
  if (route.meta.requiresAdmin) router.push({ name: 'catalog' })
}
</script>

<template>
  <header class="header">
    <div class="page header-inner">
      <RouterLink to="/" class="logo" aria-label="CineCat, inici">
        <span class="logo-mark" aria-hidden="true">C</span>
        <span class="logo-word">Cine<span class="accent">Cat</span></span>
      </RouterLink>
      <span v-if="auth.isAdmin" class="admin-badge mono">ADMIN</span>

      <nav class="nav" aria-label="Principal">
        <RouterLink to="/" class="nav-item" active-class="" exact-active-class="active">Catàleg</RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin" class="nav-item" active-class="active">Gestió</RouterLink>
        <span class="nav-item disabled" aria-disabled="true" title="Properament">Novetats</span>
        <span class="nav-item disabled" aria-disabled="true" title="Properament">Top valorades</span>
      </nav>

      <div class="session">
        <template v-if="auth.isLoggedIn">
          <span class="user-chip" :title="auth.user.email">
            <span class="avatar" aria-hidden="true">{{ auth.user.alias.charAt(0).toUpperCase() }}</span>
            <span class="alias">@{{ auth.user.alias }}</span>
          </span>
          <button class="btn btn-ghost small" type="button" @click="logout">Surt</button>
        </template>
        <RouterLink v-else :to="{ name: 'login', query: route.name === 'login' ? undefined : { redirect: route.fullPath } }" class="btn btn-ghost small">
          Inicia sessió
        </RouterLink>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 62px;
  background: rgba(13, 14, 17, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-faint);
}
.header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 28px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: -0.02em;
}
.logo-mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--accent);
  color: var(--on-accent);
  font-size: 16px;
}
.accent {
  color: var(--accent);
}
.admin-badge {
  margin-left: -16px;
  padding: 3px 8px;
  border-radius: 6px;
  background: var(--accent-15);
  border: 1px solid var(--accent-30);
  color: var(--accent);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
}
.nav {
  display: flex;
  gap: 4px;
}
.nav-item {
  padding: 7px 12px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-muted);
}
.nav-item.active {
  background: var(--raised);
  color: var(--text);
}
.nav-item.disabled {
  cursor: default;
  opacity: 0.7;
}
.session {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}
.small {
  padding: 8px 14px;
  font-size: 13px;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: var(--r-pill);
  background: var(--raised);
  border: 1px solid var(--border);
  font-size: 13px;
  font-weight: 500;
  max-width: 180px;
}
.avatar {
  flex: none;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--accent-15);
  color: var(--accent);
  font-weight: 600;
  font-size: 12px;
}
.alias {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 860px) {
  .nav-item.disabled {
    display: none;
  }
}
@media (max-width: 560px) {
  .header-inner {
    gap: 8px;
  }
  .nav-item {
    padding: 7px 8px;
  }
  .logo-word,
  .alias {
    display: none;
  }
  .admin-badge {
    margin-left: -6px;
  }
  .user-chip {
    padding-right: 4px;
  }
}
</style>
