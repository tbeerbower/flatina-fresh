<script setup>
import { onMounted, ref } from 'vue'
import { api, auth, errorMessage } from '../api'

const users = ref([])
const error = ref('')
const message = ref('')
const form = ref({ username: '', displayName: '', password: '', admin: false })

const importJson = ref(null)
const importImages = ref([])
const importing = ref(false)
const importResult = ref(null)

async function load() {
  try {
    const { data } = await api.get('/api/admin/users')
    users.value = data
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function addUser() {
  error.value = ''
  message.value = ''
  try {
    await api.post('/api/admin/users', form.value)
    message.value = `Added ${form.value.username}. Share the password with them; they can change it under their name at the top right.`
    form.value = { username: '', displayName: '', password: '', admin: false }
    load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function resetPassword(user) {
  const password = window.prompt(`New password for ${user.username} (at least 8 characters):`)
  if (!password) return
  try {
    await api.put(`/api/admin/users/${user.id}`, { password })
    message.value = `Password for ${user.username} was changed.`
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function toggleAdmin(user) {
  try {
    await api.put(`/api/admin/users/${user.id}`, { admin: !user.admin })
    load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function remove(user) {
  if (!window.confirm(`Remove ${user.username}?`)) return
  try {
    await api.delete(`/api/admin/users/${user.id}`)
    load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function runImport() {
  error.value = ''
  importResult.value = null
  if (!importJson.value) {
    error.value = 'Choose the recipes.json file.'
    return
  }
  const data = new FormData()
  data.append('recipes', importJson.value)
  for (const f of importImages.value) data.append('images', f)
  importing.value = true
  try {
    const res = await api.post('/api/admin/recipes/import', data, { timeout: 300000 })
    importResult.value = res.data
  } catch (e) {
    error.value = errorMessage(e, 'Import failed')
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>

<template>
  <h1>Admin</h1>
  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="message" class="notice">{{ message }}</p>

  <h2>Users</h2>
  <table class="card">
    <thead><tr><th>Username</th><th>Name</th><th>Role</th><th></th></tr></thead>
    <tbody>
      <tr v-for="u in users" :key="u.id">
        <td>{{ u.username }}</td>
        <td>{{ u.displayName }}</td>
        <td>{{ u.admin ? 'Admin' : 'Member' }}</td>
        <td class="actions" style="justify-content: flex-end">
          <button class="small" @click="resetPassword(u)">Reset password</button>
          <template v-if="u.username.toLowerCase() !== auth.user.username.toLowerCase()">
            <button class="small" @click="toggleAdmin(u)">{{ u.admin ? 'Make member' : 'Make admin' }}</button>
            <button class="small danger" @click="remove(u)">Remove</button>
          </template>
        </td>
      </tr>
    </tbody>
  </table>

  <h3>Add a user</h3>
  <form class="card" @submit.prevent="addUser">
    <div class="row">
      <div class="field"><label>Username</label><input v-model="form.username" required autocomplete="off" /></div>
      <div class="field"><label>Name</label><input v-model="form.displayName" /></div>
      <div class="field"><label>Password (8+ characters)</label><input v-model="form.password" type="password" minlength="8" required autocomplete="new-password" /></div>
    </div>
    <label style="display: flex; gap: 0.4rem; align-items: center"><input v-model="form.admin" type="checkbox" style="width: auto" /> Admin (can manage users and import recipes)</label>
    <button class="primary" type="submit" style="margin-top: 0.5rem">Add user</button>
  </form>

  <h2>Import recipes</h2>
  <div class="card">
    <p class="muted small" style="margin-top: 0">
      Choose a <code>recipes.json</code> bundle and the card images it lists. Recipes whose title already exists are skipped, so importing the same bundle twice is safe.
    </p>
    <div class="row">
      <div class="field"><label>recipes.json</label><input type="file" accept="application/json,.json" @change="importJson = $event.target.files[0]" /></div>
      <div class="field"><label>Card images</label><input type="file" accept="image/jpeg,image/png" multiple @change="importImages = Array.from($event.target.files)" /></div>
    </div>
    <button class="primary" :disabled="importing" @click="runImport">{{ importing ? 'Importing…' : 'Import' }}</button>
    <div v-if="importResult" class="notice">
      Imported {{ importResult.imported.length }}<span v-if="importResult.imported.length">: {{ importResult.imported.join(', ') }}</span>.
      <span v-if="importResult.skipped.length"> Skipped (already there): {{ importResult.skipped.join(', ') }}.</span>
      <span v-if="importResult.missingImages.length"> Images not uploaded: {{ importResult.missingImages.join(', ') }}.</span>
    </div>
  </div>
</template>
