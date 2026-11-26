<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage, imageUrl } from '../api'
import { formatAmount } from '../format'
import { addDays, weekLabel, weekStart } from '../dates'
import AuthImage from '../components/AuthImage.vue'
import { proteinInfo } from '../recipes'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const recipe = ref(null)
const error = ref('')
const servings = ref(2)
const thisMonday = weekStart()
const weekOptions = [0, 1, 2, 3].map((n) => addDays(thisMonday, 7 * n))
const planWeek = ref(thisMonday)
const planServings = ref(2)
const planned = ref('')
const zoomed = ref(null)

const protein = computed(() => recipe.value && proteinInfo(recipe.value.protein))
const boxItems = computed(() => (recipe.value ? recipe.value.ingredients.filter((i) => !i.pantry) : []))
const pantryItems = computed(() => (recipe.value ? recipe.value.ingredients.filter((i) => i.pantry) : []))
const noteLines = computed(() => (recipe.value && recipe.value.notes ? recipe.value.notes.split('\n').filter(Boolean) : []))

function amount(i) {
  return formatAmount(servings.value === 4 ? i.quantity4 : i.quantity2, i.unit)
}

onMounted(async () => {
  try {
    const { data } = await api.get(`/api/recipes/${props.id}`)
    recipe.value = data
  } catch (e) {
    error.value = errorMessage(e)
  }
})

async function addToPlan() {
  planned.value = ''
  try {
    // Meals are planned per week for now, on the week's Monday
    await api.post('/api/plan', { date: planWeek.value, recipeId: recipe.value.id, servings: planServings.value })
    planned.value = `Added to ${weekLabel(planWeek.value).toLowerCase()}.`
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function remove() {
  if (!window.confirm(`Delete "${recipe.value.title}"? This also removes it from your plans.`)) return
  try {
    await api.delete(`/api/recipes/${recipe.value.id}`)
    router.push('/recipes')
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function zoom(imageId) {
  zoomed.value = await imageUrl(recipe.value.id, imageId)
}
</script>

<template>
  <p v-if="error" class="error">{{ error }}</p>
  <div v-if="recipe">
    <div class="page-head">
      <div>
        <router-link to="/recipes" class="small">← Recipes</router-link>
        <h1>{{ recipe.title }}</h1>
        <div v-if="recipe.subtitle" class="muted">{{ recipe.subtitle }}</div>
        <div class="small muted" style="margin-top: 0.4rem">
          <span v-if="recipe.prepMinutes">Prep {{ recipe.prepMinutes }} min · </span>
          <span v-if="recipe.totalMinutes">Total {{ recipe.totalMinutes }} min · </span>
          <span v-if="recipe.calories">{{ recipe.calories }} calories</span>
        </div>
        <div v-if="protein" class="small" style="margin-top: 0.3rem">{{ protein.icon }} {{ protein.label }}</div>
        <div v-if="recipe.tags" style="margin-top: 0.4rem">
          <span v-for="t in recipe.tags.split(',')" :key="t" class="pill">{{ t.trim() }}</span>
        </div>
      </div>
      <div class="actions no-print">
        <router-link :to="`/recipes/${recipe.id}/edit`" class="button">Edit</router-link>
        <button class="danger" @click="remove">Delete</button>
      </div>
    </div>

    <div class="card no-print" style="margin-bottom: 1rem">
      <div class="actions">
        <strong>Add to plan</strong>
        <select v-model="planWeek" style="width: auto" aria-label="Week">
          <option v-for="w in weekOptions" :key="w" :value="w">{{ weekLabel(w) }}</option>
        </select>
        <select v-model.number="planServings" style="width: auto">
          <option :value="2">2 servings</option>
          <option :value="4">4 servings</option>
        </select>
        <button class="primary" @click="addToPlan">Add</button>
        <span v-if="planned" class="muted">{{ planned }} <router-link :to="`/weeks/${planWeek}`">See week</router-link></span>
      </div>
    </div>

    <div v-if="recipe.imageIds.length" class="gallery no-print">
      <AuthImage v-for="img in recipe.imageIds" :key="img" :recipe-id="recipe.id" :image-id="img" :alt="recipe.title" @click="zoom(img)" />
    </div>
    <p v-if="recipe.description" style="margin-top: 1rem">{{ recipe.description }}</p>

    <h2>Ingredients</h2>
    <div class="actions no-print" style="margin-bottom: 0.5rem">
      <button :class="{ primary: servings === 2 }" class="small" @click="servings = 2">2 person</button>
      <button :class="{ primary: servings === 4 }" class="small" @click="servings = 4">4 person</button>
    </div>
    <table>
      <tbody>
        <tr v-for="i in boxItems" :key="i.id">
          <td style="width: 9rem">{{ amount(i) }}</td>
          <td>{{ i.name }} <span v-if="i.allergens" class="muted small">(contains {{ i.allergens }})</span></td>
        </tr>
      </tbody>
    </table>
    <template v-if="pantryItems.length">
      <h3>From your pantry</h3>
      <table>
        <tbody>
          <tr v-for="i in pantryItems" :key="i.id">
            <td style="width: 9rem">{{ amount(i) }}</td>
            <td>{{ i.name }}</td>
          </tr>
        </tbody>
      </table>
    </template>
    <p v-if="recipe.equipment" class="small"><strong>Bust out:</strong> {{ recipe.equipment }}</p>

    <h2>Steps</h2>
    <ol class="steps">
      <li v-for="s in recipe.steps" :key="s.id">
        <strong v-if="s.title">{{ s.title }}</strong>
        <div>{{ s.instructions }}</div>
      </li>
    </ol>

    <template v-if="noteLines.length">
      <h2>Notes</h2>
      <ul>
        <li v-for="(n, idx) in noteLines" :key="idx">{{ n }}</li>
      </ul>
    </template>
  </div>
  <div v-if="zoomed" class="lightbox" @click="zoomed = null"><img :src="zoomed" alt="" /></div>
</template>
