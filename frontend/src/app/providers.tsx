import React, { useMemo, useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthContext, AuthState } from '../shared/auth/AuthContext'
import { authApi } from '../shared/api/auth'

const REFRESH_TOKEN_KEY = 'stylish.refreshToken'

function loadRefreshToken(): string | null {
  try {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  } catch {
    return null
  }
}

function saveRefreshToken(token: string | null) {
  try {
    if (!token) localStorage.removeItem(REFRESH_TOKEN_KEY)
    else localStorage.setItem(REFRESH_TOKEN_KEY, token)
  } catch {
    // ignore
  }
}

export function AppProviders({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [refreshToken, setRefreshTokenState] = useState<string | null>(() => loadRefreshToken())

  const queryClient = useMemo(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: 0, refetchOnWindowFocus: false },
          mutations: { retry: 0 },
        },
      }),
    []
  )

  const authState: AuthState = useMemo(
    () => ({
      accessToken,
      refreshToken,
      setAccessToken,
      setRefreshToken: (t) => {
        setRefreshTokenState(t)
        saveRefreshToken(t)
      },
      loginWithTokens: (pair) => {
        setAccessToken(pair.accessToken)
        setRefreshTokenState(pair.refreshToken)
        saveRefreshToken(pair.refreshToken)
      },
      logout: async () => {
        const rt = refreshToken
        setAccessToken(null)
        setRefreshTokenState(null)
        saveRefreshToken(null)
        queryClient.clear()
        if (rt) {
          try {
            await authApi.logout(rt)
          } catch {
            // ignore
          }
        }
      },
    }),
    [accessToken, refreshToken, queryClient]
  )

  return (
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={authState}>{children}</AuthContext.Provider>
    </QueryClientProvider>
  )
}

