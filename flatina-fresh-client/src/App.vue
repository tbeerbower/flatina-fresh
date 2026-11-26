<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, logout } from './api'
import banner from './assets/banner.webp'

const router = useRouter()
const route = useRoute()
const inWeeks = computed(() => route.path.startsWith('/weeks'))
const inShopping = computed(() => route.path === '/shopping' || route.path === '/products')
const menuOpen = ref(false)

// Close the menu whenever the page changes
watch(() => route.fullPath, () => { menuOpen.value = false })

function signOut() {
  menuOpen.value = false
  logout()
  router.push('/login')
}
</script>

<template>
  <header v-if="auth.token" class="topbar">
    <router-link to="/plan" class="brand">
      <img :src="banner" alt="Flatina Fresh" width="114" height="44" />
    </router-link>
    <!-- On phones this becomes the tab bar along the bottom of the screen -->
    <nav class="mainnav" aria-label="Main">
      <router-link to="/plan" exact-active-class="router-link-active" active-class="">
        <span class="tab-icon" aria-hidden="true">🍽️</span><span>This week</span>
      </router-link>
      <router-link to="/weeks" :class="{ 'router-link-active': inWeeks }">
        <span class="tab-icon" aria-hidden="true">📅</span><span>Weeks</span>
      </router-link>
      <router-link to="/recipes">
        <span class="tab-icon" aria-hidden="true">📖</span><span>Recipes</span>
      </router-link>
      <router-link to="/shopping" :class="{ 'router-link-active': inShopping }">
        <span class="tab-icon" aria-hidden="true">🛒</span><span><span class="long-label">Shopping list</span><span class="short-label">Shopping</span></span>
      </router-link>
      <router-link v-if="auth.user && auth.user.admin" to="/admin" class="wide-only">Admin</router-link>
    </nav>
    <div class="account wide-only">
      <router-link to="/family" class="muted">Family</router-link>
      <router-link to="/account" class="muted">{{ auth.user && (auth.user.displayName || auth.user.username) }}</router-link>
      <button class="link" @click="signOut">Sign out</button>
    </div>
    <button
      class="menu-button narrow-only" :aria-expanded="menuOpen" aria-controls="account-menu" aria-label="Menu"
      @click="menuOpen = !menuOpen"
    >
      <span class="menu-icon" aria-hidden="true"></span>
    </button>
    <div v-if="menuOpen" class="menu-backdrop narrow-only" @click="menuOpen = false"></div>
    <nav v-if="menuOpen" id="account-menu" class="menu-panel narrow-only" aria-label="Account">
      <div class="menu-who">
        <strong>{{ auth.user && (auth.user.displayName || auth.user.username) }}</strong>
        <div v-if="auth.user && auth.user.email" class="muted small">{{ auth.user.email }}</div>
      </div>
      <router-link to="/family">Family</router-link>
      <router-link to="/products">My products</router-link>
      <router-link to="/account">Account</router-link>
      <router-link v-if="auth.user && auth.user.admin" to="/admin">Admin</router-link>
      <button class="menu-signout" @click="signOut">Sign out</button>
    </nav>
  </header>
  <main class="page" :class="{ 'with-tabs': auth.token }">
    <router-view />
  </main>
</template>
