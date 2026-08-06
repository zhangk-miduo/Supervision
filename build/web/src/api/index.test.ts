import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './index'

describe('HTTP authentication failure handling', () => {
  const storage = new Map<string, string>()
  const replace = vi.fn()

  beforeEach(() => {
    storage.clear()
    storage.set('supervision_token', 'expired-token')
    storage.set('supervision_user', '{"username":"tester"}')
    replace.mockClear()

    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key)
    })
    vi.stubGlobal('window', {
      location: { pathname: '/dashboard', replace }
    })
  })

  it('clears the expired session and redirects to login after a 401 response', async () => {
    await expect(http.get('/tasks', {
      adapter: async (config) => Promise.reject({
        config,
        response: { status: 401 }
      })
    })).rejects.toBeTruthy()

    expect(storage.has('supervision_token')).toBe(false)
    expect(storage.has('supervision_user')).toBe(false)
    expect(replace).toHaveBeenCalledOnce()
    expect(replace).toHaveBeenCalledWith('/login')
  })
})
