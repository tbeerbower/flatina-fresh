<script setup>
import { computed } from 'vue'
import AuthImage from './AuthImage.vue'
import { LOW_CALORIE, proteinInfo } from '../recipes'

/** A recipe card: cover photo, protein badge, time and calories. Actions go in the default slot. */
const props = defineProps({
  recipe: { type: Object, required: true },
  to: { type: [String, Object], default: null },
  selected: { type: Boolean, default: false },
})
const protein = computed(() => proteinInfo(props.recipe.protein))
const coverId = computed(() => props.recipe.coverImageId)
const recipeId = computed(() => props.recipe.recipeId || props.recipe.id)
const title = computed(() => props.recipe.recipeTitle || props.recipe.title)
const subtitle = computed(() => props.recipe.recipeSubtitle ?? props.recipe.subtitle)
</script>

<template>
  <div class="recipe-card" :class="{ selected }">
    <component :is="to ? 'router-link' : 'div'" :to="to" class="card-link">
      <div class="thumb">
        <AuthImage v-if="coverId" :recipe-id="recipeId" :image-id="coverId" :alt="title" />
        <span v-if="protein" class="protein-badge">{{ protein.icon }} {{ protein.label }}</span>
        <span v-if="recipe.calories && recipe.calories <= LOW_CALORIE" class="lowcal-badge">Low cal</span>
      </div>
      <div class="body">
        <div class="title">{{ title }}</div>
        <div v-if="subtitle" class="muted small">{{ subtitle }}</div>
        <div class="small muted meta">
          <span v-if="recipe.totalMinutes">⏱ {{ recipe.totalMinutes }} min</span>
          <span v-if="recipe.calories">🔥 {{ recipe.calories }} cal</span>
        </div>
      </div>
    </component>
    <div v-if="$slots.default" class="card-actions">
      <slot />
    </div>
  </div>
</template>
