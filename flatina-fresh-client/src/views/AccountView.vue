<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth, errorMessage, updateProfile } from '../api'

const route = useRoute()
const router = useRouter()

/** Accounts made before emails land here until they add one. */
const needsEmail = computed(() => !auth.user.email)

const name = ref(auth.user.displayName || auth.user.username)
const email = ref(auth.user.email || '')
const profileMessage = ref('')
const profileError = ref('')
const savingProfile = ref(false)

async function saveProfile() {
  profileError.value = ''
  profileMessage.value = ''
  savingProfile.value = true
  const wasMissing = needsEmail.value
  try {
    await updateProfile({ name: name.value.trim(), email: email.value.trim() })
    if (wasMissing) {
      router.replace(route.query.next || '/plan')
      return
    }
    profileMessage.value = 'Saved.'
  } catch (e) {
    profileError.value = errorMessage(e)
  } finally {
    savingProfile.value = false
  }
}

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
  <p class="muted">
    <template v-if="auth.user.email">Signed in as {{ auth.user.email }}</template>
    <template v-else>Signed in</template>{{ auth.user.admin ? ' (admin)' : '' }}
  </p>

  <form class="card" style="max-width: 420px" @submit.prevent="saveProfile">
    <h2 style="margin-top: 0">{{ needsEmail ? 'Add your email' : 'Your name and email' }}</h2>
    <p v-if="needsEmail" class="notice small">
      Flatina Fresh now signs you in with your email instead of a username. Add yours to keep going; next time, sign in
      with it and your same password.
    </p>
    <div class="field">
      <label for="acct-name">Name (what the app calls you)</label>
      <input id="acct-name" v-model="name" required maxlength="100" autocomplete="name" />
    </div>
    <div class="field">
      <label for="acct-email">Email (you sign in with it)</label>
      <input id="acct-email" v-model="email" type="email" required autocomplete="email" autocapitalize="none" />
    </div>
    <p v-if="profileError" class="error">{{ profileError }}</p>
    <p v-if="profileMessage" class="notice">{{ profileMessage }}</p>
    <button class="primary" type="submit" :disabled="savingProfile">{{ needsEmail ? 'Save and continue' : 'Save' }}</button>
  </form>

  <form v-if="!needsEmail" class="card" style="max-width: 420px; margin-top: 1rem" @submit.prevent="change">
    <h2 style="margin-top: 0">Change password</h2>
    <div class="field"><label>Current password</label><input v-model="currentPassword" type="password" required autocomplete="current-password" /></div>
    <div class="field"><label>New password (8+ characters)</label><input v-model="newPassword" type="password" minlength="8" required autocomplete="new-password" /></div>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="message" class="notice">{{ message }}</p>
    <button class="primary" type="submit">Change password</button>
  </form>
</template>
