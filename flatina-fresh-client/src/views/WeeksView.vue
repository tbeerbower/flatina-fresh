<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, errorMessage } from '../api'
import { addDays, formatRange, isIsoDate, isPastWeek, weekLabel, weekStart } from '../dates'
import AuthImage from '../components/AuthImage.vue'

/**
 * A carousel of week cards. The centered week is the selected one and shows larger; scroll, swipe, use the
 * arrows or the keyboard to move. Clicking the selected week (or "Open") goes to that week's page.
 */
const SPAN = 26 // weeks either side of the starting week
const MINI_LIMIT = 6

const route = useRoute()
const router = useRouter()
const thisWeek = weekStart()
const initial = weekStart(isIsoDate(route.query.week) ? route.query.week : thisWeek)
const weeks = ref([])
const selected = ref(initial)
const error = ref('')
const track = ref(null)
let frame = 0

const selectedWeek = computed(() => weeks.value.find((w) => w.start === selected.value))
const selectedIndex = computed(() => weeks.value.findIndex((w) => w.start === selected.value))

function cardEl(start) {
  return track.value && track.value.querySelector(`[data-week="${start}"]`)
}

function center(start, smooth = true) {
  const el = cardEl(start)
  if (!el || !track.value) return
  const left = el.offsetLeft + el.offsetWidth / 2 - track.value.clientWidth / 2
  track.value.scrollTo({ left, behavior: smooth ? 'smooth' : 'auto' })
}

/** Selected = the card nearest the middle of the track. */
function onScroll() {
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(() => {
    const t = track.value
    if (!t) return
    const mid = t.scrollLeft + t.clientWidth / 2
    let best = null
    let bestDist = Infinity
    for (const el of t.querySelectorAll('[data-week]')) {
      const dist = Math.abs(el.offsetLeft + el.offsetWidth / 2 - mid)
      if (dist < bestDist) {
        bestDist = dist
        best = el.dataset.week
      }
    }
    if (best && best !== selected.value) {
      selected.value = best
      router.replace({ query: { week: best } })
    }
  })
}

function step(delta) {
  const next = weeks.value[selectedIndex.value + delta]
  if (next) center(next.start)
}

function pick(week) {
  if (week.start === selected.value) router.push(`/weeks/${week.start}`)
  else center(week.start)
}

function lowerFirst(text) {
  return text.charAt(0).toLowerCase() + text.slice(1)
}

function onKey(e) {
  if (e.target && ['INPUT', 'SELECT', 'TEXTAREA'].includes(e.target.tagName)) return
  if (e.key === 'ArrowLeft') step(-1)
  else if (e.key === 'ArrowRight') step(1)
  else if (e.key === 'Enter' && selectedWeek.value) router.push(`/weeks/${selected.value}`)
}

onMounted(async () => {
  window.addEventListener('keydown', onKey)
  try {
    const { data } = await api.get('/api/plan/weeks', { params: { start: addDays(initial, -7 * SPAN), count: 2 * SPAN + 1 } })
    weeks.value = data
    await nextTick()
    center(initial, false)
  } catch (e) {
    error.value = errorMessage(e)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  cancelAnimationFrame(frame)
})
</script>

<template>
  <div class="page-head">
    <div>
      <h1>Weeks</h1>
      <p class="muted">Scroll through your weeks. Pick one to see its recipes or add meals.</p>
    </div>
    <div class="actions">
      <button :disabled="selected === thisWeek" @click="center(thisWeek)">Today</button>
    </div>
  </div>
  <p v-if="error" class="error">{{ error }}</p>

  <div class="carousel">
    <button class="carousel-nav prev" aria-label="Previous week" :disabled="selectedIndex <= 0" @click="step(-1)">‹</button>
    <div ref="track" class="carousel-track" @scroll.passive="onScroll">
      <article
        v-for="w in weeks" :key="w.start" :data-week="w.start" class="week-card"
        :class="{ selected: w.start === selected, current: w.start === thisWeek }"
        :aria-current="w.start === selected ? 'true' : undefined" @click="pick(w)"
      >
        <header>
          <div class="week-name">{{ weekLabel(w.start) }}</div>
          <div class="muted small">{{ formatRange(w.start, addDays(w.start, 6)) }}</div>
        </header>
        <div v-if="w.entries.length" class="mini-grid">
          <div v-for="e in w.entries.slice(0, MINI_LIMIT)" :key="e.id" class="mini-card">
            <div class="mini-thumb">
              <AuthImage v-if="e.coverImageId" :recipe-id="e.recipeId" :image-id="e.coverImageId" :alt="e.recipeTitle" />
            </div>
            <div class="mini-title">{{ e.recipeTitle }}</div>
          </div>
          <div v-if="w.entries.length > MINI_LIMIT" class="mini-more">+{{ w.entries.length - MINI_LIMIT }} more</div>
        </div>
        <div v-else class="mini-empty">No meals yet</div>
        <footer class="small muted">{{ w.entries.length }} meal{{ w.entries.length === 1 ? '' : 's' }}</footer>
      </article>
    </div>
    <button class="carousel-nav next" aria-label="Next week" :disabled="selectedIndex >= weeks.length - 1" @click="step(1)">›</button>
  </div>

  <div v-if="selectedWeek" class="carousel-actions">
    <router-link :to="`/weeks/${selected}`" class="button primary">Open {{ lowerFirst(weekLabel(selected)) }}</router-link>
    <router-link v-if="!isPastWeek(selected)" :to="`/weeks/${selected}/add`" class="button">+ Add meals</router-link>
  </div>
</template>
