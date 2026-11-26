<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import AuthImage from '../components/AuthImage.vue'
import { PROTEINS } from '../recipes'

const props = defineProps({ id: { type: String, default: null } })
const router = useRouter()
const error = ref('')
const saving = ref(false)
const newImages = ref([])
const recipe = ref({
  title: '', subtitle: '', description: '', source: 'HelloFresh',
  prepMinutes: null, totalMinutes: null, calories: null, protein: null,
  tags: '', equipment: '', notes: '', ingredients: [], steps: [], imageIds: [],
})

onMounted(async () => {
  if (!props.id) {
    addIngredient()
    addStep()
    return
  }
  try {
    const { data } = await api.get(`/api/recipes/${props.id}`)
    recipe.value = data
  } catch (e) {
    error.value = errorMessage(e)
  }
})

function addIngredient(pantry = false) {
  recipe.value.ingredients.push({ name: '', quantity2: null, quantity4: null, unit: '', allergens: '', pantry })
}
function addStep() {
  recipe.value.steps.push({ title: '', instructions: '' })
}
function move(list, index, delta) {
  const target = index + delta
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
}

async function removeImage(imageId) {
  if (!window.confirm('Remove this image?')) return
  await api.delete(`/api/recipes/${recipe.value.id}/images/${imageId}`)
  recipe.value.imageIds = recipe.value.imageIds.filter((i) => i !== imageId)
}

async function save() {
  error.value = ''
  saving.value = true
  const payload = {
    ...recipe.value,
    ingredients: recipe.value.ingredients.filter((i) => i.name && i.name.trim()),
    steps: recipe.value.steps.filter((s) => s.instructions && s.instructions.trim()),
  }
  try {
    const { data } = props.id
      ? await api.put(`/api/recipes/${props.id}`, payload)
      : await api.post('/api/recipes', payload)
    if (newImages.value.length) {
      const form = new FormData()
      for (const f of newImages.value) form.append('images', f)
      await api.post(`/api/recipes/${data.id}/images`, form)
    }
    router.push(`/recipes/${data.id}`)
  } catch (e) {
    error.value = errorMessage(e, 'Could not save')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-head">
    <div>
      <router-link :to="id ? `/recipes/${id}` : '/recipes'" class="small">← Back</router-link>
      <h1>{{ id ? 'Edit recipe' : 'New recipe' }}</h1>
    </div>
    <div class="actions">
      <button class="primary" :disabled="saving" @click="save">Save</button>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>

  <div class="card">
    <div class="field"><label>Title</label><input v-model="recipe.title" required /></div>
    <div class="field"><label>Subtitle</label><input v-model="recipe.subtitle" placeholder="with …" /></div>
    <div class="field"><label>Description</label><textarea v-model="recipe.description" rows="2"></textarea></div>
    <div class="row">
      <div class="field"><label>Prep (min)</label><input v-model.number="recipe.prepMinutes" type="number" min="0" /></div>
      <div class="field"><label>Total (min)</label><input v-model.number="recipe.totalMinutes" type="number" min="0" /></div>
      <div class="field"><label>Calories</label><input v-model.number="recipe.calories" type="number" min="0" /></div>
      <div class="field"><label>Source</label><input v-model="recipe.source" /></div>
    </div>
    <div class="row">
      <div class="field">
        <label>Protein</label>
        <select v-model="recipe.protein">
          <option :value="null">Work it out from the ingredients</option>
          <option v-for="p in PROTEINS" :key="p.value" :value="p.value">{{ p.icon }} {{ p.label }}</option>
        </select>
      </div>
      <div class="field"><label>Tags (comma separated)</label><input v-model="recipe.tags" /></div>
      <div class="field"><label>Equipment</label><input v-model="recipe.equipment" /></div>
    </div>
  </div>

  <h2>Ingredients</h2>
  <p class="muted small">Amounts for the 2-person and 4-person columns on the card. Tick "pantry" for "Bust out" items you keep at home.</p>
  <div class="edit-list">
    <div v-for="(i, idx) in recipe.ingredients" :key="idx" class="edit-row ing-row">
      <input v-model="i.name" placeholder="Ingredient" aria-label="Ingredient" />
      <label class="ing-field"><span class="ing-label">2 people</span><input v-model.number="i.quantity2" type="number" step="any" min="0" inputmode="decimal" placeholder="2p" /></label>
      <label class="ing-field"><span class="ing-label">4 people</span><input v-model.number="i.quantity4" type="number" step="any" min="0" inputmode="decimal" placeholder="4p" /></label>
      <label class="ing-field"><span class="ing-label">Unit</span><input v-model="i.unit" placeholder="unit" /></label>
      <input v-model="i.allergens" placeholder="allergens" aria-label="Allergens" />
      <label style="display: flex; gap: 0.3rem; align-items: center; margin: 0"><input v-model="i.pantry" type="checkbox" style="width: auto" /> pantry</label>
      <div class="actions">
        <button class="small" title="Move up" @click="move(recipe.ingredients, idx, -1)">↑</button>
        <button class="small danger" title="Remove" @click="recipe.ingredients.splice(idx, 1)">✕</button>
      </div>
    </div>
  </div>
  <div class="actions">
    <button class="small" @click="addIngredient(false)">+ Ingredient</button>
    <button class="small" @click="addIngredient(true)">+ Pantry item</button>
  </div>

  <h2>Steps</h2>
  <div class="edit-list">
    <div v-for="(s, idx) in recipe.steps" :key="idx" class="edit-row step-row">
      <input v-model="s.title" :placeholder="`Step ${idx + 1} title`" />
      <textarea v-model="s.instructions" rows="3" placeholder="Instructions"></textarea>
      <div class="actions">
        <button class="small" title="Move up" @click="move(recipe.steps, idx, -1)">↑</button>
        <button class="small danger" title="Remove" @click="recipe.steps.splice(idx, 1)">✕</button>
      </div>
    </div>
  </div>
  <button class="small" @click="addStep">+ Step</button>

  <h2>Notes</h2>
  <textarea v-model="recipe.notes" rows="4" placeholder="Tips, swaps, one per line"></textarea>

  <h2>Images</h2>
  <div v-if="recipe.imageIds && recipe.imageIds.length" class="gallery" style="margin-bottom: 0.75rem">
    <div v-for="img in recipe.imageIds" :key="img" style="position: relative">
      <AuthImage :recipe-id="recipe.id" :image-id="img" alt="" />
      <button class="small danger" style="position: absolute; top: 6px; right: 6px" @click="removeImage(img)">✕</button>
    </div>
  </div>
  <input type="file" accept="image/jpeg,image/png" multiple @change="newImages = Array.from($event.target.files)" />

  <div class="actions" style="margin-top: 1.5rem">
    <button class="primary" :disabled="saving" @click="save">Save</button>
  </div>
</template>
