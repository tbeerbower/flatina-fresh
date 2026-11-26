<script setup>
import { useRouter } from 'vue-router'
import { auth, logout } from './api'

const router = useRouter()

function signOut() {
  logout()
  router.push('/login')
}
</script>

<template>
  <header v-if="auth.token" class="topbar">
    <router-link to="/plan" class="brand">
      <img src="/favicon.svg" alt="" width="28" height="28" />
      <span>Flatina Fresh</span>
    </router-link>
    <nav class="mainnav">
      <router-link to="/plan">This week</router-link>
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
