import React, { createContext, useContext } from 'react'
import { TokenPair } from '../api/auth'

export type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  setAccessToken: (t: string | null) => void
  setRefreshToken: (t: string | null) => void
  loginWithTokens: (pair: TokenPair) => void
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthContext is not configured')
  return ctx
}

