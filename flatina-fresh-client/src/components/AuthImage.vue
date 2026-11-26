<script setup>
import { ref, watchEffect } from 'vue'
import { imageUrl } from '../api'

const props = defineProps({
  recipeId: { type: [Number, String], required: true },
  imageId: { type: [Number, String], required: true },
  alt: { type: String, default: '' },
})
const src = ref(null)

watchEffect(async () => {
  src.value = null
  try {
    src.value = await imageUrl(props.recipeId, props.imageId)
  } catch {
    src.value = null
  }
})
</script>

<template>
  <img v-if="src" :src="src" :alt="alt" />
</template>
