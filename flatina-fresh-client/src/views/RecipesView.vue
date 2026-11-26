<script setup>
import { onMounted, ref, watch } from 'vue'
import { api, errorMessage } from '../api'
import AuthImage from '../components/AuthImage.vue'

const recipes = ref([])
const q = ref('')
const loading = ref(true)
const error = ref('')
let timer = null

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/api/recipes', { params: { q: q.value || undefined } })
    recipes.value = data
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

watch(q, () => {
  clearTimeout(timer)
  timer = setTimeout(load, 250)
})
onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <h1>Recipes</h1>
      <p class="muted">{{ recipes.length }} recipe{{ recipes.length === 1 ? '' : 's' }} in your collection</p>
    </div>
    <div class="actions">
      <input v-model="q" type="search" placeholder="Search recipes, tags…" style="width: 240px" />
      <router-link to="/recipes/scan" class="button primary">Scan a card</router-link>
      <router-link to="/recipes/new" class="button">Add by hand</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="!loading && !recipes.length" class="card muted">
    No recipes yet. Scan a HelloFresh card, add one by hand, or import a bundle from the Admin page.
  </p>
  <div class="recipe-grid">
    <router-link v-for="r in recipes" :key="r.id" :to="`/recipes/${r.id}`" class="recipe-card">
      <div class="thumb">
        <AuthImage v-if="r.coverImageId" :recipe-id="r.id" :image-id="r.coverImageId" :alt="r.title" />
      </div>
      <div class="body">
        <div class="title">{{ r.title }}</div>
        <div v-if="r.subtitle" class="muted small">{{ r.subtitle }}</div>
        <div class="small muted" style="margin-top: 0.35rem">
          <span v-if="r.totalMinutes">{{ r.totalMinutes }} min</span>
          <span v-if="r.totalMinutes && r.calories"> · </span>
          <span v-if="r.calories">{{ r.calories }} cal</span>
        </div>
      </div>
    </router-link>
  </div>
</template>
