import { apiFetch, apiFetchForm } from './http'
import { TokenPair } from './auth'

export type ItemType = 'TOP' | 'BOTTOM' | 'HAT' | 'SHOES' | 'OUTERWEAR' | 'ACCESSORY'

export type Profile = {
  id: string
  userId: string
  displayName: string
  createdAt: string
}

export type UserPhoto = {
  id: string
  imageObjectKey: string
  createdAt: string
}

export type Item = {
  id: string
  name: string
  color: string
  type: ItemType
  imageObjectKey: string
  createdAt: string
}

export type Look = {
  id: string
  name: string | null
  sourceUserPhotoId: string
  itemIds: string[]
  resultImageObjectKey: string
  resultImageUrl: string
  createdAt: string
}

export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

export const wardrobeApi = {
  async getProfileMe(auth: AuthCtx) {
    return apiFetch<Profile>('/api/wardrobe/profiles/me', authOpts(auth))
  },

  async createProfile(auth: AuthCtx, displayName: string) {
    return apiFetch<Profile>('/api/wardrobe/profiles', {
      ...authOpts(auth),
      method: 'POST',
      body: { displayName },
    })
  },

  async uploadUserPhoto(auth: AuthCtx, file: File) {
    const fd = new FormData()
    fd.append('file', file)
    return apiFetchForm<UserPhoto>('/api/wardrobe/user-photos', fd, authOpts(auth))
  },

  async getUserPhoto(auth: AuthCtx, id: string) {
    return apiFetch<UserPhoto>(`/api/wardrobe/user-photos/${id}`, authOpts(auth))
  },

  async createItem(auth: AuthCtx, meta: { name: string; color: string; type: ItemType }, file: File) {
    const fd = new FormData()
    fd.append('metadata', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
    fd.append('file', file)
    return apiFetchForm<Item>('/api/wardrobe/items', fd, authOpts(auth))
  },

  async getItem(auth: AuthCtx, id: string) {
    return apiFetch<Item>(`/api/wardrobe/items/${id}`, authOpts(auth))
  },

  async deleteItem(auth: AuthCtx, id: string) {
    return apiFetch<void>(`/api/wardrobe/items/${id}`, { ...authOpts(auth), method: 'DELETE' })
  },

  async listItems(
    auth: AuthCtx,
    params: { page: number; size: number; sort?: string; type?: ItemType; color?: string; nameLike?: string }
  ) {
    const qs = new URLSearchParams()
    qs.set('page', String(params.page))
    qs.set('size', String(params.size))
    if (params.sort) qs.set('sort', params.sort)
    if (params.type) qs.set('type', params.type)
    if (params.color) qs.set('color', params.color)
    if (params.nameLike) qs.set('nameLike', params.nameLike)
    return apiFetch<Page<Item>>(`/api/wardrobe/items?${qs.toString()}`, authOpts(auth))
  },

  async generateLook(auth: AuthCtx, req: { userPhotoId: string; itemIds: string[]; name?: string }) {
    return apiFetch<Look>('/api/wardrobe/looks/generate', { ...authOpts(auth), method: 'POST', body: req })
  },

  async getLook(auth: AuthCtx, id: string) {
    return apiFetch<Look>(`/api/wardrobe/looks/${id}`, authOpts(auth))
  },

  async renameLook(auth: AuthCtx, id: string, name: string) {
    return apiFetch<Look>(`/api/wardrobe/looks/${id}`, { ...authOpts(auth), method: 'PATCH', body: { name } })
  },
}

export type AuthCtx = {
  accessToken: string | null
  refreshToken: string | null
  onTokens: (pair: TokenPair) => void
  onUnauthorized: () => void
}

function authOpts(auth: AuthCtx) {
  return {
    accessToken: auth.accessToken,
    refreshToken: auth.refreshToken,
    onTokens: auth.onTokens,
    onUnauthorized: auth.onUnauthorized,
  }
}

