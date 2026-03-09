import React, { useEffect, useMemo } from 'react'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { useAuth } from '../shared/auth/AuthContext'
import { authApi } from '../shared/api/auth'
import { AuthPage } from '../pages/AuthPage/AuthPage'
import { DashboardPage } from '../pages/DashboardPage/DashboardPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const auth = useAuth()
  if (!auth.accessToken) return <Navigate to="/auth" replace />
  return <>{children}</>
}

export function AppRouter() {
  const auth = useAuth()

  // тихий авто-refresh при перезагрузке страницы (если есть refreshToken)
  useEffect(() => {
    let cancelled = false
    async function run() {
      if (auth.accessToken) return
      if (!auth.refreshToken) return
      try {
        const pair = await authApi.refresh(auth.refreshToken)
        if (cancelled) return
        auth.loginWithTokens(pair)
      } catch {
        if (cancelled) return
        auth.setAccessToken(null)
        auth.setRefreshToken(null)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const router = useMemo(
    () =>
      createBrowserRouter([
        { path: '/', element: <Navigate to="/app" replace /> },
        { path: '/auth', element: <AuthPage /> },
        {
          path: '/app',
          element: (
            <RequireAuth>
              <DashboardPage />
            </RequireAuth>
          ),
        },
      ]),
    []
  )

  return <RouterProvider router={router} />
}

