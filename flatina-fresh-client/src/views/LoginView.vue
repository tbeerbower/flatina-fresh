<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, errorMessage } from '../api'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await login(username.value, password.value)
    router.replace(route.query.next || '/plan')
  } catch (e) {
    error.value = errorMessage(e, 'Could not sign in')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login card">
    <div style="text-align: center">
      <img src="/favicon.svg" alt="" width="56" height="56" />
      <h1>Flatina Fresh</h1>
      <p class="muted">Home-cooked HelloFresh, planned for the week.</p>
    </div>
    <form @submit.prevent="submit">
      <div class="field">
        <label for="u">Username</label>
        <input id="u" v-model="username" autocomplete="username" required />
      </div>
      <div class="field">
        <label for="p">Password</label>
        <input id="p" v-model="password" type="password" autocomplete="current-password" required />
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="primary" type="submit" :disabled="busy" style="width: 100%">Sign in</button>
    </form>
  </div>
</template>
