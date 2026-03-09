import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, UseQueryResult } from '@tanstack/react-query'
import { AuthCtx, Item, ItemType, Look, Page, Profile, wardrobeApi } from '../../../shared/api/wardrobe'
import { Card } from '../../../shared/ui/Card/Card'
import { Button } from '../../../shared/ui/Button/Button'
import { Input } from '../../../shared/ui/Input/Input'
import { Select } from '../../../shared/ui/Select/Select'
import common from './common.module.css'
import styles from './LooksCard.module.css'

const LAST_USER_PHOTO_ID = 'stylish.lastUserPhotoId'
const LAST_LOOK_ID = 'stylish.lastLookId'

function load(key: string) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function save(key: string, value: string | null) {
  try {
    if (!value) localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch {
    // ignore
  }
}

const TYPES: ItemType[] = ['TOP', 'BOTTOM', 'HAT', 'SHOES', 'OUTERWEAR', 'ACCESSORY']
const TYPE_LABEL: Record<ItemType, string> = {
  TOP: 'TOP (верх)',
  BOTTOM: 'BOTTOM (низ)',
  HAT: 'HAT (головной убор)',
  SHOES: 'SHOES (обувь)',
  OUTERWEAR: 'OUTERWEAR (верхняя одежда)',
  ACCESSORY: 'ACCESSORY (аксессуар)',
}

export function LooksCard({
  authCtx,
  profileQuery,
  onToast,
}: {
  authCtx: AuthCtx
  profileQuery: UseQueryResult<Profile, Error>
  onToast: (t: string | null) => void
}) {
  const canUse = useMemo(() => profileQuery.isSuccess, [profileQuery.isSuccess])

  const [userPhotoId, setUserPhotoId] = useState(() => load(LAST_USER_PHOTO_ID) ?? '')
  const [name, setName] = useState('')
  const [selectedByType, setSelectedByType] = useState<Record<ItemType, string>>(() => ({
    TOP: '',
    BOTTOM: '',
    HAT: '',
    SHOES: '',
    OUTERWEAR: '',
    ACCESSORY: '',
  }))

  const [look, setLook] = useState<Look | null>(null)
  const [lastLookId, setLastLookId] = useState<string>(() => load(LAST_LOOK_ID) ?? '')
  const [rename, setRename] = useState('')

  const itemsQuery = useQuery({
    queryKey: ['itemsForLooks'],
    queryFn: () => wardrobeApi.listItems(authCtx, { page: 0, size: 200, sort: 'createdAt,desc' }),
    enabled: canUse,
  })

  const items: Item[] = (itemsQuery.data as Page<Item> | undefined)?.content ?? []
  const byType = useMemo(() => {
    const m: Record<ItemType, Item[]> = {
      TOP: [],
      BOTTOM: [],
      HAT: [],
      SHOES: [],
      OUTERWEAR: [],
      ACCESSORY: [],
    }
    for (const it of items) m[it.type].push(it)
    return m
  }, [items])

  const generateMutation = useMutation({
    mutationFn: async () => {
      const chosen = TYPES.map((t) => selectedByType[t]).filter(Boolean)
      const unique = Array.from(new Set(chosen))
      if (!userPhotoId.trim()) throw new Error('Нужен userPhotoId (загрузи фото и вставь id)')
      if (unique.length === 0) throw new Error('Выбери хотя бы 1 вещь')
      return wardrobeApi.generateLook(authCtx, {
        userPhotoId: userPhotoId.trim(),
        itemIds: unique,
        name: name.trim() || undefined,
      })
    },
    onSuccess: (l) => {
      setLook(l)
      setRename(l.name ?? '')
      setLastLookId(l.id)
      save(LAST_LOOK_ID, l.id)
      onToast('Лук создан')
      setTimeout(() => onToast(null), 1400)
    },
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось сгенерировать')
      setTimeout(() => onToast(null), 1800)
    },
  })

  const loadLookMutation = useMutation({
    mutationFn: (id: string) => wardrobeApi.getLook(authCtx, id),
    onSuccess: (l) => {
      setLook(l)
      setRename(l.name ?? '')
    },
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось загрузить look')
      setTimeout(() => onToast(null), 1800)
    },
  })

  const renameMutation = useMutation({
    mutationFn: async () => {
      if (!look) throw new Error('Нет look')
      if (!rename.trim()) throw new Error('Имя обязательно')
      return wardrobeApi.renameLook(authCtx, look.id, rename.trim())
    },
    onSuccess: (l) => setLook(l),
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось переименовать')
      setTimeout(() => onToast(null), 1800)
    },
  })

  useEffect(() => {
    save(LAST_USER_PHOTO_ID, userPhotoId.trim() ? userPhotoId.trim() : null)
  }, [userPhotoId])

  return (
    <Card title="Собрать лук (looks/generate)">
      {!canUse ? (
        <div className={common.muted}>Сначала создай профиль и загрузи user-photo.</div>
      ) : (
        <div className={styles.wrap}>
          <div className={common.grid2}>
            <Input
              label="userPhotoId"
              placeholder="UUID из /user-photos (сохраняем последний автоматически)"
              value={userPhotoId}
              onChange={(e) => setUserPhotoId(e.target.value)}
            />
            <Input
              label="Название (опционально)"
              placeholder="Например: вечерний лук"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div className={styles.types}>
            {TYPES.map((t) => {
              const options = [
                { value: '', label: '— не выбрано —' },
                ...byType[t].map((it) => ({
                  value: it.id,
                  label: `${it.name} (${it.color})`,
                })),
              ]
              return (
                <Select
                  key={t}
                  label={TYPE_LABEL[t]}
                  value={selectedByType[t]}
                  options={options}
                  onChange={(v) => setSelectedByType((s) => ({ ...s, [t]: v }))}
                />
              )
            })}
          </div>

          <div className={common.row}>
            <Button onClick={() => generateMutation.mutate()} disabled={generateMutation.isPending}>
              Сгенерировать
            </Button>
            <span className={common.muted}>Проверка: не больше 1 вещи на тип</span>
          </div>

          <div className={common.divider} />

          <div className={styles.saved}>
            <Input
              label="Последний lookId"
              value={lastLookId}
              onChange={(e) => setLastLookId(e.target.value)}
              placeholder="UUID look"
            />
            <Button variant="ghost" onClick={() => lastLookId && loadLookMutation.mutate(lastLookId)}>
              Load look
            </Button>
          </div>

          {look ? (
            <div className={styles.result}>
              <div>
                <div className={common.muted}>
                  lookId: <code>{look.id}</code>
                </div>
                <div className={common.muted}>
                  createdAt: {new Date(look.createdAt).toLocaleString()}
                </div>
                <div className={common.muted}>
                  items: {look.itemIds.length}
                </div>

                <div className={common.divider} />

                <div className={common.row}>
                  <Input
                    label="Переименовать"
                    value={rename}
                    onChange={(e) => setRename(e.target.value)}
                  />
                  <Button onClick={() => renameMutation.mutate()} disabled={renameMutation.isPending}>
                    Save
                  </Button>
                </div>
              </div>

              <div>
                <img className={common.img} src={look.resultImageUrl} alt="look" />
              </div>
            </div>
          ) : (
            <div className={common.muted}>Результат генерации появится здесь.</div>
          )}
        </div>
      )}
    </Card>
  )
}

