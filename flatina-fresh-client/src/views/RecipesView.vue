<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'
import RecipeBrowser from '../components/RecipeBrowser.vue'
import RecipeCard from '../components/RecipeCard.vue'

const recipes = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const { data } = await api.get('/api/recipes')
    recipes.value = data
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-head">
    <div>
      <h1>Recipes</h1>
      <p class="muted">{{ recipes.length }} recipe{{ recipes.length === 1 ? '' : 's' }} in your collection</p>
    </div>
    <div class="actions">
      <router-link to="/recipes/scan" class="button primary">Scan a card</router-link>
      <router-link to="/recipes/new" class="button">Add by hand</router-link>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="!loading && !recipes.length" class="card muted">
    No recipes yet. Scan a HelloFresh card, add one by hand, or import a bundle from the Admin page.
  </p>
  <RecipeBrowser v-else v-slot="{ recipe }" :recipes="recipes">
    <RecipeCard :recipe="recipe" :to="`/recipes/${recipe.id}`" />
  </RecipeBrowser>
</template>
