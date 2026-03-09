import { useMemo, useState } from 'react'
import { useMutation, UseQueryResult } from '@tanstack/react-query'
import { Card } from '../../../shared/ui/Card/Card'
import { Button } from '../../../shared/ui/Button/Button'
import { AuthCtx, Profile, UserPhoto, wardrobeApi } from '../../../shared/api/wardrobe'
import common from './common.module.css'

const LAST_USER_PHOTO_ID = 'stylish.lastUserPhotoId'

function loadLastPhotoId() {
  try {
    return localStorage.getItem(LAST_USER_PHOTO_ID)
  } catch {
    return null
  }
}

function saveLastPhotoId(id: string | null) {
  try {
    if (!id) localStorage.removeItem(LAST_USER_PHOTO_ID)
    else localStorage.setItem(LAST_USER_PHOTO_ID, id)
  } catch {
    // ignore
  }
}

export function UserPhotoCard({
  authCtx,
  profileQuery,
  onToast,
}: {
  authCtx: AuthCtx
  profileQuery: UseQueryResult<Profile, Error>
  onToast: (t: string | null) => void
}) {
  const [lastId, setLastId] = useState<string | null>(() => loadLastPhotoId())
  const [file, setFile] = useState<File | null>(null)
  const [photo, setPhoto] = useState<UserPhoto | null>(null)

  const upload = useMutation({
    mutationFn: async () => {
      if (!file) throw new Error('Выбери файл')
      return wardrobeApi.uploadUserPhoto(authCtx, file)
    },
    onSuccess: (p) => {
      setPhoto(p)
      setLastId(p.id)
      saveLastPhotoId(p.id)
      onToast('Фото загружено')
      setTimeout(() => onToast(null), 1400)
    },
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось загрузить фото')
      setTimeout(() => onToast(null), 1800)
    },
  })

  const canUse = useMemo(() => profileQuery.isSuccess, [profileQuery.isSuccess])

  return (
    <Card title="Моё фото (user-photos)">
      {!canUse ? (
        <div className={common.muted}>Сначала создай профиль.</div>
      ) : (
        <div className={common.grid2}>
          <div>
            <div className={common.row}>
              <input
                type="file"
                accept="image/*"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
              <Button
                onClick={() => upload.mutate()}
                disabled={upload.isPending || !file}
              >
                Upload
              </Button>
            </div>
            <div className={common.divider} />
            <div className={common.muted}>
              lastUserPhotoId: <code>{lastId ?? '(нет)'}</code>
            </div>
            {photo ? (
              <div className={common.muted}>
                objectKey: <code>{photo.imageObjectKey}</code>
              </div>
            ) : null}
          </div>

          <div>
            {photo ? (
              <img
                className={common.img}
                src={`/files/${photo.imageObjectKey}`}
                alt="user"
              />
            ) : (
              <div className={common.muted}>После upload тут появится превью.</div>
            )}
          </div>
        </div>
      )}
    </Card>
  )
}

