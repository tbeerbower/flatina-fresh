import axios from 'axios'
import { reactive } from 'vue'

const TOKEN_KEY = 'flatina.token'
const USER_KEY = 'flatina.user'

const baseURL = (window.APP_CONFIG && window.APP_CONFIG.API_URL) || 'http://localhost:8080'

export const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY),
  user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
})

// Sessions from before sign-in by email (no user id) no longer work on the server: start over at the login page.
if (auth.token && !(auth.user && auth.user.id)) {
  auth.token = null
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  if (auth.token) config.headers.Authorization = `Bearer ${auth.token}`
  return config
})

api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response && error.response.status === 401 && auth.token) {
      logout()
      window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

/** Signs in with an email (or, for an account made before emails, its username) and password. */
export async function login(email, password) {
  const { data } = await api.post('/api/auth/login', { email: email.trim(), password })
  setSession(data)
}

/** Changes the signed-in user's name and/or email. */
export async function updateProfile(changes) {
  const { data } = await api.put('/api/auth/me', changes)
  auth.user = data
  localStorage.setItem(USER_KEY, JSON.stringify(data))
  return data
}

/** Creates an account from a family invite link and signs it in. */
export async function acceptInvite(token, account) {
  const { data } = await api.post(`/api/invites/${encodeURIComponent(token)}/accept`, account)
  setSession(data)
}

function setSession(data) {
  auth.token = data.token
  auth.user = data.user
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(USER_KEY, JSON.stringify(data.user))
}

export function logout() {
  auth.token = null
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** Readable message from an axios error. */
export function errorMessage(error, fallback = 'Something went wrong') {
  const data = error && error.response && error.response.data
  if (data && typeof data === 'object' && data.message) return data.message
  if (error && error.message) return error.message
  return fallback
}

const imageCache = new Map()

/** Images need the auth header, so fetch them as blobs and hand back object URLs. */
export async function imageUrl(recipeId, imageId) {
  const key = `${recipeId}/${imageId}`
  if (!imageCache.has(key)) {
    imageCache.set(
      key,
      api
        .get(`/api/recipes/${recipeId}/images/${imageId}`, { responseType: 'blob' })
        .then((r) => URL.createObjectURL(r.data))
        .catch((e) => {
          imageCache.delete(key)
          throw e
        }),
    )
  }
  return imageCache.get(key)
}
