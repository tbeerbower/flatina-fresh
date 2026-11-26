<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, logout } from './api'
import banner from './assets/banner.webp'

const router = useRouter()
const route = useRoute()
const inWeeks = computed(() => route.path.startsWith('/weeks'))

function signOut() {
  logout()
  router.push('/login')
}
</script>

<template>
  <header v-if="auth.token" class="topbar">
    <router-link to="/plan" class="brand">
      <img :src="banner" alt="Flatina Fresh" width="114" height="44" />
    </router-link>
    <nav class="mainnav">
      <router-link to="/plan" exact-active-class="router-link-active" active-class="">This week</router-link>
      <router-link to="/weeks" :class="{ 'router-link-active': inWeeks }">Weeks</router-link>
      <router-link to="/recipes">Recipes</router-link>
      <router-link to="/shopping">Shopping list</router-link>
      <router-link v-if="auth.user && auth.user.admin" to="/admin">Admin</router-link>
    </nav>
    <div class="account">
      <router-link to="/account" class="muted">{{ auth.user && (auth.user.displayName || auth.user.username) }}</router-link>
      <button class="link" @click="signOut">Sign out</button>
    </div>
  </header>
  <main class="page">
    <router-view />
  </main>
</template>
