import React, { useEffect, useMemo, useState } from 'react'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { useAuth } from '../shared/auth/AuthContext'
import { authApi } from '../shared/api/auth'
import { AuthPage } from '../pages/AuthPage/AuthPage'
import { DashboardPage } from '../pages/DashboardPage/DashboardPage'

function RequireAuth({ booting, children }: { booting: boolean; children: React.ReactNode }) {
  const auth = useAuth()
  if (!auth.accessToken) {
    if (booting && auth.refreshToken) {
      return (
        <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', color: 'rgba(255,255,255,0.75)' }}>
          Восстанавливаем сессию…
        </div>
      )
    }
    return <Navigate to="/auth" replace />
  }
  return <>{children}</>
}

export function AppRouter() {
  const auth = useAuth()
  const [booting, setBooting] = useState(true)

  // тихий авто-refresh при перезагрузке страницы (если есть refreshToken)
  useEffect(() => {
    let cancelled = false
    async function run() {
      if (auth.accessToken) return
      if (!auth.refreshToken) return
      let attempt = 0
      // если сервисы в compose рестартятся, auth может быть временно недоступен
      while (!cancelled) {
        try {
          const pair = await authApi.refresh(auth.refreshToken)
          if (cancelled) return
          auth.loginWithTokens(pair)
          return
        } catch (e: any) {
          if (cancelled) return
          if (e?.status === 401) {
            // токен реально протух/отозван — чистим
            auth.setAccessToken(null)
            auth.setRefreshToken(null)
            return
          }
          attempt += 1
          const delay = Math.min(3000, 400 + attempt * 400)
          await new Promise((r) => setTimeout(r, delay))
        }
      }
    }
    void run().finally(() => {
      if (!cancelled) setBooting(false)
    })
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
            <RequireAuth booting={booting}>
              <DashboardPage />
            </RequireAuth>
          ),
        },
      ]),
    []
  )

  return <RouterProvider router={router} />
}

