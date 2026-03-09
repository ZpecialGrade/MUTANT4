export type TokenPair = {
  tokenType: string
  accessToken: string
  refreshToken: string
  accessExpiresInSeconds: number
}

export const authApi = {
  async register(email: string, password: string) {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) throw new Error((await res.json().catch(() => null))?.detail ?? 'Register failed')
    return res.json()
  },

  async login(email: string, password: string): Promise<TokenPair> {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) throw new Error((await res.json().catch(() => null))?.detail ?? 'Login failed')
    return res.json()
  },

  async refresh(refreshToken: string): Promise<TokenPair> {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) {
      const err: any = new Error('Refresh failed')
      err.status = res.status
      throw err
    }
    return res.json()
  },

  async logout(refreshToken: string) {
    await fetch('/api/auth/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
  },

  async me(accessToken: string) {
    const res = await fetch('/api/auth/me', {
      method: 'GET',
      headers: { Accept: 'application/json', Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error('Unauthorized')
    return res.json()
  },
}

