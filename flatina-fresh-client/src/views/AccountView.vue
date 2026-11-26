<script setup>
import { ref } from 'vue'
import { api, auth, errorMessage } from '../api'

const currentPassword = ref('')
const newPassword = ref('')
const message = ref('')
const error = ref('')

async function change() {
  error.value = ''
  message.value = ''
  try {
    await api.put('/api/auth/password', { currentPassword: currentPassword.value, newPassword: newPassword.value })
    message.value = 'Password changed.'
    currentPassword.value = ''
    newPassword.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}
</script>

<template>
  <h1>{{ auth.user.displayName || auth.user.username }}</h1>
  <p class="muted">Signed in as {{ auth.user.username }}{{ auth.user.admin ? ' (admin)' : '' }}</p>
  <form class="card" style="max-width: 420px" @submit.prevent="change">
    <h2 style="margin-top: 0">Change password</h2>
    <div class="field"><label>Current password</label><input v-model="currentPassword" type="password" required autocomplete="current-password" /></div>
    <div class="field"><label>New password (8+ characters)</label><input v-model="newPassword" type="password" minlength="8" required autocomplete="new-password" /></div>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="message" class="notice">{{ message }}</p>
    <button class="primary" type="submit">Change password</button>
  </form>
</template>
