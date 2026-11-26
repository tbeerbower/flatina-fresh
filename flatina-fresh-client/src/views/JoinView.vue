<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { acceptInvite, api, auth, errorMessage, logout } from '../api'
import pancakePals from '../assets/pancake-pals.webp'

/** Where an invite link lands: create an account that joins the inviting family. */
const props = defineProps({ token: { type: String, required: true } })
const router = useRouter()
const invite = ref(null)
const loadError = ref('')
const form = ref({ username: '', email: '', password: '' })
const error = ref('')
const busy = ref(false)

onMounted(async () => {
  try {
    const { data } = await api.get(`/api/invites/${encodeURIComponent(props.token)}`)
    invite.value = data
  } catch (e) {
    loadError.value = errorMessage(e, 'This invite link does not work.')
  }
})

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await acceptInvite(props.token, { ...form.value, email: form.value.email.trim(), username: form.value.username.trim() })
    router.replace('/plan')
  } catch (e) {
    error.value = errorMessage(e, 'Could not create your account')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login card">
    <div style="text-align: center">
      <img :src="pancakePals" alt="Flatina Fresh" class="login-art" width="240" height="358" />
    </div>
    <p v-if="loadError" class="error">{{ loadError }}</p>
    <template v-else-if="invite">
      <h2 style="margin-top: 0">Join {{ invite.familyName }}</h2>
      <p class="muted">
        <template v-if="invite.invitedBy">{{ invite.invitedBy }} invited you to plan meals together.</template>
        <template v-else>You're invited to plan meals together.</template>
        Make an account to get started.
      </p>
      <p v-if="auth.token" class="notice small">
        You're signed in as {{ auth.user && (auth.user.displayName || auth.user.username) }}. This link makes a new account.
        <button class="link" @click="logout()">Sign out</button> first if you're on a shared device.
      </p>
      <form @submit.prevent="submit">
        <div class="field">
          <label for="n">Your name</label>
          <input id="n" v-model="form.username" autocomplete="name" required maxlength="100" />
        </div>
        <div class="field">
          <label for="e">Email (you'll sign in with it)</label>
          <input id="e" v-model="form.email" type="email" autocomplete="email" autocapitalize="none" required />
        </div>
        <div class="field">
          <label for="p">Password (8+ characters)</label>
          <input id="p" v-model="form.password" type="password" minlength="8" autocomplete="new-password" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="primary" type="submit" :disabled="busy" style="width: 100%">Create account and join</button>
      </form>
    </template>
  </div>
</template>
