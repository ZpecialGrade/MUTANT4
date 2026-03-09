import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import styles from './DashboardPage.module.css'
import { Button } from '../../shared/ui/Button/Button'
import { Card } from '../../shared/ui/Card/Card'
import { useAuth } from '../../shared/auth/AuthContext'
import { wardrobeApi } from '../../shared/api/wardrobe'
import { ProfileCard } from './components/ProfileCard'
import { UserPhotoCard } from './components/UserPhotoCard'
import { ItemsCard } from './components/ItemsCard'
import { LooksCard } from './components/LooksCard'

export function DashboardPage() {
  const auth = useAuth()
  const [toast, setToast] = useState<string | null>(null)

  const authCtx = useMemo(
    () => ({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      onTokens: auth.loginWithTokens,
      onUnauthorized: () => void auth.logout(),
    }),
    [auth]
  )

  const profileQuery = useQuery({
    queryKey: ['profileMe'],
    queryFn: () => wardrobeApi.getProfileMe(authCtx),
    enabled: !!auth.accessToken,
  })

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.brand}>
          <div className={styles.logo}>Stylish</div>
          <div className={styles.small}>учебный фронт</div>
        </div>
        <div className={styles.actions}>
          <Button
            variant="ghost"
            onClick={() => {
              navigator.clipboard?.writeText(String(auth.accessToken ?? ''))
              setToast('accessToken скопирован (если браузер разрешил)')
              setTimeout(() => setToast(null), 1800)
            }}
          >
            Copy access
          </Button>
          <Button variant="danger" onClick={() => void auth.logout()}>
            Выйти
          </Button>
        </div>
      </header>

      <div className={styles.grid}>
        <ProfileCard authCtx={authCtx} profileQuery={profileQuery} onToast={setToast} />
        <UserPhotoCard authCtx={authCtx} profileQuery={profileQuery} onToast={setToast} />
        <ItemsCard authCtx={authCtx} profileQuery={profileQuery} onToast={setToast} />
        <LooksCard authCtx={authCtx} profileQuery={profileQuery} onToast={setToast} />
      </div>

      {toast ? (
        <div className={styles.toast}>
          <Card>{toast}</Card>
        </div>
      ) : null}
    </div>
  )
}

