import { useMemo } from 'react'
import { useMutation, UseQueryResult } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Card } from '../../../shared/ui/Card/Card'
import { Button } from '../../../shared/ui/Button/Button'
import { Input } from '../../../shared/ui/Input/Input'
import { AuthCtx, Profile, wardrobeApi } from '../../../shared/api/wardrobe'
import common from './common.module.css'

const schema = z.object({ displayName: z.string().min(1, 'Введите имя').max(80) })
type Values = z.infer<typeof schema>

export function ProfileCard({
  authCtx,
  profileQuery,
  onToast,
}: {
  authCtx: AuthCtx
  profileQuery: UseQueryResult<Profile, Error>
  onToast: (t: string | null) => void
}) {
  const createForm = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { displayName: '' },
  })

  const createMutation = useMutation({
    mutationFn: (displayName: string) => wardrobeApi.createProfile(authCtx, displayName),
    onSuccess: () => {
      profileQuery.refetch()
      onToast('Профиль создан')
      setTimeout(() => onToast(null), 1400)
    },
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось создать профиль')
      setTimeout(() => onToast(null), 1800)
    },
  })

  const view = useMemo(() => {
    if (profileQuery.isPending) return { kind: 'loading' as const }
    if (profileQuery.isSuccess) return { kind: 'ok' as const, profile: profileQuery.data }
    if (profileQuery.isError) return { kind: 'missing' as const, error: profileQuery.error }
    return { kind: 'loading' as const }
  }, [profileQuery])

  return (
    <Card title="Профиль">
      {view.kind === 'loading' ? (
        <div className={common.muted}>Загрузка…</div>
      ) : view.kind === 'ok' ? (
        <div className={common.grid2}>
          <div>
            <div className={common.pill}>displayName: {view.profile.displayName}</div>
          </div>
          <div>
            <div className={common.pill}>profileId: {view.profile.id}</div>
          </div>
          <div>
            <div className={common.pill}>userId: {view.profile.userId}</div>
          </div>
          <div>
            <div className={common.pill}>createdAt: {new Date(view.profile.createdAt).toLocaleString()}</div>
          </div>
        </div>
      ) : (
        <form
          className={common.grid2}
          onSubmit={createForm.handleSubmit(async (v) => {
            await createMutation.mutateAsync(v.displayName)
          })}
        >
          <Input
            label="Display name"
            placeholder="Например: Dima"
            {...createForm.register('displayName')}
            error={createForm.formState.errors.displayName?.message}
          />
          <div className={common.row} style={{ alignItems: 'end' }}>
            <Button type="submit" disabled={createMutation.isPending}>
              Создать профиль
            </Button>
            <span className={common.muted}>
              (Если 404 — это ожидаемо, просто создай профиль)
            </span>
          </div>
        </form>
      )}
    </Card>
  )
}

