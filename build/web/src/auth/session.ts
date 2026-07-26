import { ref } from 'vue'
import type { LoginResponse } from '@/api'

const TOKEN_KEY = 'supervision_token'
const USER_KEY = 'supervision_user'

function readStoredUser(): LoginResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as LoginResponse
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const currentUser = ref<LoginResponse | null>(readStoredUser())

export function setSession(session: LoginResponse) {
  localStorage.setItem(TOKEN_KEY, session.token)
  localStorage.setItem(USER_KEY, JSON.stringify(session))
  currentUser.value = session
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  currentUser.value = null
}
