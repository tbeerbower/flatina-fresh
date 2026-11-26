<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '../api'
import { formatDay, toIso } from '../dates'

/** Your family: everyone in it shares the meal plan. Invite someone with a link that creates their account. */
const family = ref(null)
const name = ref('')
const editingName = ref(false)
const newLink = ref('')
const copied = ref(false)
const error = ref('')
const busy = ref(false)

function linkFor(token) {
  return `${window.location.origin}/join/${token}`
}

function expires(invite) {
  return formatDay(toIso(new Date(invite.expiresAt)), { weekday: 'short', month: 'short', day: 'numeric' })
}

async function load() {
  try {
    const { data } = await api.get('/api/family')
    family.value = data
    name.value = data.name
    error.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function rename() {
  try {
    const { data } = await api.put('/api/family', { name: name.value })
    family.value = data
    editingName.value = false
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function invite() {
  busy.value = true
  copied.value = false
  try {
    const { data } = await api.post('/api/family/invites')
    newLink.value = linkFor(data.token)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}

async function copy(link) {
  try {
    await navigator.clipboard.writeText(link)
    newLink.value = link
    copied.value = true
  } catch {
    newLink.value = link
  }
}

async function share(link) {
  if (navigator.share) {
    try {
      await navigator.share({ title: 'Join our Flatina Fresh family', url: link })
      return
    } catch {
      // cancelled or unavailable: fall back to copying
    }
  }
  copy(link)
}

async function revoke(inv) {
  try {
    await api.delete(`/api/family/invites/${inv.id}`)
    if (newLink.value === linkFor(inv.token)) newLink.value = ''
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function removeMember(m) {
  if (!confirm(`Take ${m.displayName} out of the family? They keep their account but won't see this meal plan.`)) return
  try {
    await api.delete(`/api/family/members/${m.id}`)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

onMounted(load)
</script>

<template>
  <div v-if="family">
    <div class="page-head">
      <div>
        <h1 v-if="!editingName">{{ family.name }}</h1>
        <form v-else class="actions" @submit.prevent="rename">
          <input v-model="name" maxlength="200" required aria-label="Family name" />
          <button class="primary" type="submit">Save</button>
          <button type="button" @click="editingName = false; name = family.name">Cancel</button>
        </form>
        <p class="muted">Everyone in your family shares the meal plan and can change this week and the weeks ahead.</p>
      </div>
      <div v-if="!editingName" class="actions">
        <button @click="editingName = true">Rename</button>
      </div>
    </div>
    <p v-if="error" class="error">{{ error }}</p>

    <h2>Members</h2>
    <div class="card family-list">
      <div v-for="m in family.members" :key="m.id" class="family-row">
        <div>
          <strong>{{ m.displayName }}</strong><span v-if="m.you" class="pill" style="margin-left: 0.4rem">You</span>
          <div class="muted small">{{ m.username }}</div>
        </div>
        <button v-if="!m.you" class="small danger" @click="removeMember(m)">Remove</button>
      </div>
    </div>

    <h2>Invite someone</h2>
    <div class="card">
      <p class="muted small" style="margin-top: 0">
        Make a link and send it to them however you like. Opening it lets them pick a username and password, and their
        account joins this family. Each link works once and expires after 7 days.
      </p>
      <button class="primary" :disabled="busy" @click="invite">{{ busy ? 'Making link…' : 'Make an invite link' }}</button>
      <div v-if="newLink" class="invite-link">
        <input :value="newLink" readonly aria-label="Invite link" @focus="$event.target.select()" />
        <button @click="share(newLink)">Share</button>
        <button @click="copy(newLink)">{{ copied ? '✓ Copied' : 'Copy' }}</button>
      </div>

      <template v-if="family.invites.length">
        <h3>Open invites</h3>
        <div class="family-list">
          <div v-for="inv in family.invites" :key="inv.id" class="family-row">
            <div class="small">
              Made by {{ inv.createdBy || 'a former member' }} · expires {{ expires(inv) }}
            </div>
            <div class="actions">
              <button class="small" @click="copy(linkFor(inv.token))">Copy link</button>
              <button class="small danger" @click="revoke(inv)">Cancel</button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
  <p v-else-if="error" class="error">{{ error }}</p>
</template>
