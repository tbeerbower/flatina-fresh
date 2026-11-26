<script setup>
import { reactive, ref } from 'vue'
import { api, errorMessage } from '../api'

/** Edits the saved product for one ingredient. `product` is the saved one, if any; `ingredientName` names a new one. */
const props = defineProps({
  product: { type: Object, default: null },
  ingredientName: { type: String, default: '' },
  suggestedUnit: { type: String, default: '' },
})
const emit = defineEmits(['saved', 'deleted', 'cancel'])

const p = props.product
const form = reactive({
  ingredientName: p ? p.ingredientName : props.ingredientName,
  brand: (p && p.brand) || '',
  productName: (p && p.productName) || '',
  packageSize: p && p.packageSize != null ? String(p.packageSize) : '',
  packageUnit: (p && p.packageUnit) || (p ? '' : props.suggestedUnit || ''),
  packageLabel: (p && p.packageLabel) || '',
  storeItem: (p && p.storeItem) || '',
  staple: !!(p && p.staple),
})
const busy = ref(false)
const error = ref('')

const UNITS = ['oz', 'lb', 'g', 'fl oz', 'ml', 'cup', 'tbsp', 'tsp', 'clove', 'slice', 'can', 'bunch', 'each']
const LABELS = ['bottle', 'jar', 'can', 'bag', 'box', 'pack', 'tub', 'carton', 'bunch', 'head', 'loaf', 'each']

async function save() {
  busy.value = true
  error.value = ''
  const size = form.packageSize === '' ? null : Number(form.packageSize)
  if (size !== null && !(size > 0)) {
    error.value = 'Package size should be a number above 0'
    busy.value = false
    return
  }
  const body = { ...form, packageSize: size, packageUnit: form.packageUnit === 'each' ? '' : form.packageUnit }
  try {
    const { data } = p ? await api.put(`/api/products/${p.id}`, body) : await api.post('/api/products', body)
    emit('saved', data)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}

async function remove() {
  busy.value = true
  try {
    await api.delete(`/api/products/${p.id}`)
    emit('deleted', p)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="product-editor" @submit.prevent="save">
    <div class="row">
      <div class="field">
        <label>Ingredient</label>
        <input v-model="form.ingredientName" required maxlength="200" />
      </div>
      <div class="field">
        <label>Brand</label>
        <input v-model="form.brand" maxlength="100" placeholder="e.g. Kikkoman" />
      </div>
      <div class="field">
        <label>Product</label>
        <input v-model="form.productName" maxlength="200" placeholder="e.g. Less Sodium Soy Sauce" />
      </div>
    </div>
    <div class="row">
      <div class="field">
        <label>Package holds</label>
        <div class="size-inputs">
          <input v-model="form.packageSize" inputmode="decimal" placeholder="10" aria-label="Package size" />
          <input v-model="form.packageUnit" list="product-units" placeholder="fl oz" aria-label="Package unit" />
        </div>
      </div>
      <div class="field">
        <label>Package is a</label>
        <input v-model="form.packageLabel" list="product-labels" maxlength="50" placeholder="bottle" />
      </div>
      <div class="field">
        <label>Store item # or link</label>
        <input v-model="form.storeItem" maxlength="500" placeholder="Giant item number, UPC or URL" />
      </div>
    </div>
    <datalist id="product-units"><option v-for="u in UNITS" :key="u" :value="u" /></datalist>
    <datalist id="product-labels"><option v-for="l in LABELS" :key="l" :value="l" /></datalist>
    <p class="muted small" style="margin: 0 0 0.6rem">
      Package size lets the list count how many to buy. Use the recipe's kind of unit: volume (tbsp, cup, fl oz),
      weight (oz, lb, g), or a count like 10 cloves for a head of garlic.
    </p>
    <label class="check"><input v-model="form.staple" type="checkbox" /> I always have this at home (leave it off the order)</label>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="actions">
      <button class="primary small" type="submit" :disabled="busy">Save product</button>
      <button class="small" type="button" :disabled="busy" @click="emit('cancel')">Cancel</button>
      <button v-if="p" class="small danger" type="button" :disabled="busy" @click="remove">Forget product</button>
    </div>
  </form>
</template>
