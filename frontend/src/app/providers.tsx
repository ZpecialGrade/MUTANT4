import React, { useMemo, useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthContext, AuthState } from '../shared/auth/AuthContext'
import { authApi } from '../shared/api/auth'

const REFRESH_TOKEN_KEY = 'stylish.refreshToken'
const ACCESS_TOKEN_KEY = 'stylish.accessToken'

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

function loadAccessToken(): string | null {
  try {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
}

function saveAccessToken(token: string | null) {
  try {
    if (!token) sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    else sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  } catch {
    // ignore
  }
}

export function AppProviders({ children }: { children: React.ReactNode }) {
  const [accessToken, setAccessTokenState] = useState<string | null>(() => loadAccessToken())
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
      setAccessToken: (t) => {
        setAccessTokenState(t)
        saveAccessToken(t)
      },
      setRefreshToken: (t) => {
        setRefreshTokenState(t)
        saveRefreshToken(t)
      },
      loginWithTokens: (pair) => {
        setAccessTokenState(pair.accessToken)
        saveAccessToken(pair.accessToken)
        setRefreshTokenState(pair.refreshToken)
        saveRefreshToken(pair.refreshToken)
      },
      logout: async () => {
        const rt = refreshToken
        setAccessTokenState(null)
        saveAccessToken(null)
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

