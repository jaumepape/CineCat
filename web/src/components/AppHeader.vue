<script setup>
// Capçalera fixa de 62px. "Novetats", "Top valorades" i "Inicia sessió" són
// al disseny però fora d'aquesta fase: es mostren desactivats.
</script>

<template>
  <header class="header">
    <div class="page header-inner">
      <RouterLink to="/" class="logo" aria-label="CineCat, inici">
        <span class="logo-mark" aria-hidden="true">C</span>
        <span class="logo-word">Cine<span class="accent">Cat</span></span>
      </RouterLink>

      <nav class="nav" aria-label="Principal">
        <RouterLink to="/" class="nav-item" active-class="" exact-active-class="active">Catàleg</RouterLink>
        <span class="nav-item disabled" aria-disabled="true" title="Properament">Novetats</span>
        <span class="nav-item disabled" aria-disabled="true" title="Properament">Top valorades</span>
      </nav>

      <button class="btn btn-ghost login" type="button" disabled title="Arriba a la Fase 4">
        Inicia sessió
      </button>
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
.login {
  margin-left: auto;
  padding: 8px 14px;
  font-size: 13px;
}
@media (max-width: 720px) {
  .nav-item.disabled {
    display: none;
  }
  .header-inner {
    gap: 14px;
  }
}
</style>
