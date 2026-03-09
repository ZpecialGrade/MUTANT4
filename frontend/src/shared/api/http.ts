import { authApi, TokenPair } from './auth'

type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE'

export type HttpClientOptions = {
  method?: HttpMethod
  headers?: Record<string, string>
  body?: unknown
  accessToken?: string | null
  refreshToken?: string | null
  onTokens?: (pair: TokenPair) => void
  onUnauthorized?: () => void
  retryOnUnauthorized?: boolean
}

let refreshInFlight: Promise<TokenPair> | null = null

export async function apiFetch<T>(
  url: string,
  opts: HttpClientOptions = {}
): Promise<T> {
  const {
    method = 'GET',
    headers = {},
    body,
    accessToken,
    refreshToken,
    onTokens,
    onUnauthorized,
    retryOnUnauthorized = true,
  } = opts

  const res = await fetch(url, {
    method,
    headers: buildHeaders(headers, accessToken, body),
    body: body == null ? undefined : JSON.stringify(body),
  })

  if (res.status !== 401 || !retryOnUnauthorized) {
    return handleResponse<T>(res)
  }

  if (!refreshToken) {
    onUnauthorized?.()
    return handleResponse<T>(res)
  }

  try {
    const pair = await refreshOnce(refreshToken)
    onTokens?.(pair)

    const retry = await fetch(url, {
      method,
      headers: buildHeaders(headers, pair.accessToken, body),
      body: body == null ? undefined : JSON.stringify(body),
    })
    return handleResponse<T>(retry)
  } catch {
    onUnauthorized?.()
    return handleResponse<T>(res)
  }
}

export async function apiFetchForm<T>(
  url: string,
  formData: FormData,
  opts: Omit<HttpClientOptions, 'body' | 'headers'> & { headers?: Record<string, string> } = {}
): Promise<T> {
  const {
    method = 'POST',
    headers = {},
    accessToken,
    refreshToken,
    onTokens,
    onUnauthorized,
    retryOnUnauthorized = true,
  } = opts

  const res = await fetch(url, {
    method,
    headers: buildMultipartHeaders(headers, accessToken),
    body: formData,
  })

  if (res.status !== 401 || !retryOnUnauthorized) {
    return handleResponse<T>(res)
  }

  if (!refreshToken) {
    onUnauthorized?.()
    return handleResponse<T>(res)
  }

  try {
    const pair = await refreshOnce(refreshToken)
    onTokens?.(pair)

    const retry = await fetch(url, {
      method,
      headers: buildMultipartHeaders(headers, pair.accessToken),
      body: formData,
    })
    return handleResponse<T>(retry)
  } catch {
    onUnauthorized?.()
    return handleResponse<T>(res)
  }
}

function buildHeaders(
  headers: Record<string, string>,
  accessToken: string | null | undefined,
  body: unknown
) {
  const h: Record<string, string> = { ...headers }
  if (accessToken) h.Authorization = `Bearer ${accessToken}`
  if (body != null) h['Content-Type'] = h['Content-Type'] ?? 'application/json'
  h.Accept = h.Accept ?? 'application/json'
  return h
}

function buildMultipartHeaders(
  headers: Record<string, string>,
  accessToken: string | null | undefined
) {
  const h: Record<string, string> = { ...headers }
  if (accessToken) h.Authorization = `Bearer ${accessToken}`
  // Content-Type НЕ задаём — браузер сам проставит boundary
  h.Accept = h.Accept ?? 'application/json'
  return h
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T

  const contentType = res.headers.get('content-type') ?? ''
  const isJson = contentType.includes('application/json') || contentType.includes('+json')

  if (res.ok) {
    if (!isJson) return (await res.blob()) as unknown as T
    return (await res.json()) as T
  }

  if (isJson) {
    const problem = await res.json().catch(() => null)
    const message =
      (problem && (problem.detail || problem.message)) ||
      `HTTP ${res.status}`
    throw new Error(message)
  }

  const text = await res.text().catch(() => '')
  throw new Error(text || `HTTP ${res.status}`)
}

async function refreshOnce(refreshToken: string) {
  if (!refreshInFlight) {
    refreshInFlight = authApi.refresh(refreshToken).finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

