<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage } from '../api'

const router = useRouter()
const front = ref(null)
const back = ref(null)
const busy = ref(false)
const error = ref('')

async function scan() {
  error.value = ''
  const form = new FormData()
  for (const f of [front.value, back.value]) if (f) form.append('images', f)
  if (!form.has('images')) {
    error.value = 'Choose the front and back scans of the card.'
    return
  }
  busy.value = true
  try {
    const { data } = await api.post('/api/recipes/scan', form, { timeout: 240000 })
    router.push(`/recipes/${data.id}/edit`)
  } catch (e) {
    error.value = errorMessage(e, 'Scanning failed')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="page-head">
    <div>
      <router-link to="/recipes" class="small">← Recipes</router-link>
      <h1>Scan a recipe card</h1>
      <p class="muted">Upload both sides of a HelloFresh card. Claude reads it and you get a chance to check the result before it's done.</p>
    </div>
  </div>
  <div class="card">
    <div class="row wide-fields">
      <div class="field">
        <label>Front (title, photo, ingredients)</label>
        <input type="file" accept="image/jpeg,image/png" @change="front = $event.target.files[0]" />
      </div>
      <div class="field">
        <label>Back (steps)</label>
        <input type="file" accept="image/jpeg,image/png" @change="back = $event.target.files[0]" />
      </div>
    </div>
    <p class="muted small">Tip: make sure the whole card is in the scan. Edges that are cut off can't be read.</p>
    <p v-if="error" class="error">{{ error }}</p>
    <button class="primary" :disabled="busy" @click="scan">{{ busy ? 'Reading card… (up to a minute)' : 'Scan card' }}</button>
  </div>
</template>
