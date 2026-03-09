import { useMemo, useState } from 'react'
import { useMutation, useQuery, UseQueryResult } from '@tanstack/react-query'
import { AuthCtx, Item, ItemType, Page, Profile, wardrobeApi } from '../../../shared/api/wardrobe'
import { Card } from '../../../shared/ui/Card/Card'
import { Button } from '../../../shared/ui/Button/Button'
import { Input } from '../../../shared/ui/Input/Input'
import { Select } from '../../../shared/ui/Select/Select'
import common from './common.module.css'
import styles from './ItemsCard.module.css'

const itemTypeOptions: { value: ItemType; label: string }[] = [
  { value: 'TOP', label: 'TOP (верх)' },
  { value: 'BOTTOM', label: 'BOTTOM (низ)' },
  { value: 'HAT', label: 'HAT (головной убор)' },
  { value: 'SHOES', label: 'SHOES (обувь)' },
  { value: 'OUTERWEAR', label: 'OUTERWEAR (верхняя одежда)' },
  { value: 'ACCESSORY', label: 'ACCESSORY (аксессуар)' },
]

const sortOptions = [
  { value: 'createdAt,desc', label: 'Новые → старые' },
  { value: 'createdAt,asc', label: 'Старые → новые' },
  { value: 'name,asc', label: 'Имя A→Z' },
  { value: 'name,desc', label: 'Имя Z→A' },
]

export function ItemsCard({
  authCtx,
  profileQuery,
  onToast,
}: {
  authCtx: AuthCtx
  profileQuery: UseQueryResult<Profile, Error>
  onToast: (t: string | null) => void
}) {
  const canUse = useMemo(() => profileQuery.isSuccess, [profileQuery.isSuccess])

  const [createName, setCreateName] = useState('')
  const [createColor, setCreateColor] = useState('')
  const [createType, setCreateType] = useState<ItemType>('TOP')
  const [createFile, setCreateFile] = useState<File | null>(null)

  const [page, setPage] = useState(0)
  const [size, setSize] = useState(8)
  const [sort, setSort] = useState(sortOptions[0].value)
  const [filterType, setFilterType] = useState<ItemType | ''>('')
  const [filterColor, setFilterColor] = useState('')
  const [filterNameLike, setFilterNameLike] = useState('')

  const listQuery = useQuery({
    queryKey: ['items', { page, size, sort, filterType, filterColor, filterNameLike }],
    queryFn: () =>
      wardrobeApi.listItems(authCtx, {
        page,
        size,
        sort,
        type: filterType || undefined,
        color: filterColor || undefined,
        nameLike: filterNameLike || undefined,
      }),
    enabled: canUse,
  })

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!createName.trim()) throw new Error('Название обязательно')
      if (!createColor.trim()) throw new Error('Цвет обязателен')
      if (!createFile) throw new Error('Нужно выбрать файл')
      return wardrobeApi.createItem(
        authCtx,
        { name: createName.trim(), color: createColor.trim(), type: createType },
        createFile
      )
    },
    onSuccess: () => {
      setCreateName('')
      setCreateColor('')
      setCreateFile(null)
      listQuery.refetch()
      onToast('Вещь создана')
      setTimeout(() => onToast(null), 1400)
    },
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось создать вещь')
      setTimeout(() => onToast(null), 1800)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => wardrobeApi.deleteItem(authCtx, id),
    onSuccess: () => {
      listQuery.refetch()
    },
    onError: (e: any) => {
      onToast(e?.message ?? 'Не удалось удалить')
      setTimeout(() => onToast(null), 1800)
    },
  })

  const data: Page<Item> | null = listQuery.data ?? null

  return (
    <Card title="Вещи (items)">
      {!canUse ? (
        <div className={common.muted}>Сначала создай профиль.</div>
      ) : (
        <div className={styles.wrap}>
          <div className={styles.create}>
            <div className={common.grid2}>
              <Input
                label="Название"
                placeholder="Футболка"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
              />
              <Input
                label="Цвет"
                placeholder="Черный"
                value={createColor}
                onChange={(e) => setCreateColor(e.target.value)}
              />
              <Select
                label="Тип"
                value={createType}
                options={itemTypeOptions}
                onChange={(v) => setCreateType(v as ItemType)}
              />
              <label className={styles.file}>
                <div className={styles.fileLabel}>Фото</div>
                <input type="file" accept="image/*" onChange={(e) => setCreateFile(e.target.files?.[0] ?? null)} />
                <div className={styles.fileHint}>{createFile ? createFile.name : 'Выбери изображение'}</div>
              </label>
            </div>
            <div className={common.row}>
              <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}>
                Добавить вещь
              </Button>
              <span className={common.muted}>multipart: metadata + file</span>
            </div>
          </div>

          <div className={common.divider} />

          <div className={styles.filters}>
            <Select
              label="Сортировка"
              value={sort}
              options={sortOptions}
              onChange={(v) => {
                setPage(0)
                setSort(v)
              }}
            />
            <Select
              label="Фильтр: type"
              value={filterType || ''}
              options={[{ value: '', label: 'Любой' }, ...itemTypeOptions.map((o) => ({ value: o.value, label: o.label }))]}
              onChange={(v) => {
                setPage(0)
                setFilterType((v || '') as any)
              }}
            />
            <Input
              label="Фильтр: color"
              placeholder="Напр. черный"
              value={filterColor}
              onChange={(e) => {
                setPage(0)
                setFilterColor(e.target.value)
              }}
            />
            <Input
              label="Фильтр: nameLike"
              placeholder="Напр. футбол"
              value={filterNameLike}
              onChange={(e) => {
                setPage(0)
                setFilterNameLike(e.target.value)
              }}
            />
          </div>

          <div className={styles.listHeader}>
            <div className={common.muted}>
              {listQuery.isPending ? 'Загрузка…' : data ? `Элементов: ${data.totalElements}` : ''}
            </div>
            <div className={styles.pager}>
              <Button variant="ghost" disabled={!data || data.first} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                ←
              </Button>
              <div className={common.pill}>
                page {data?.number ?? 0} / {Math.max(0, (data?.totalPages ?? 1) - 1)}
              </div>
              <Button variant="ghost" disabled={!data || data.last} onClick={() => setPage((p) => p + 1)}>
                →
              </Button>
              <Select
                value={String(size)}
                options={[
                  { value: '6', label: '6' },
                  { value: '8', label: '8' },
                  { value: '12', label: '12' },
                ]}
                onChange={(v) => {
                  setPage(0)
                  setSize(Number(v))
                }}
              />
            </div>
          </div>

          <div className={styles.grid}>
            {data?.content?.map((it) => (
              <div key={it.id} className={styles.item}>
                <img className={styles.thumb} src={`/files/${it.imageObjectKey}`} alt={it.name} />
                <div className={styles.meta}>
                  <div className={styles.name}>{it.name}</div>
                  <div className={common.muted}>
                    {it.type} · {it.color}
                  </div>
                  <div className={common.muted}>
                    <code>{it.id}</code>
                  </div>
                </div>
                <div className={styles.itemActions}>
                  <Button variant="danger" onClick={() => deleteMutation.mutate(it.id)} disabled={deleteMutation.isPending}>
                    Delete
                  </Button>
                </div>
              </div>
            ))}
            {data && data.content.length === 0 ? <div className={common.muted}>Пока пусто.</div> : null}
          </div>
        </div>
      )}
    </Card>
  )
}

