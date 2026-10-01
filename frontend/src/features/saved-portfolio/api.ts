import type {
  SavedPortfolio,
  SaveTargetsRequest,
  WalletSession,
} from './contracts'

export class WalletApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'WalletApiError'
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isPortfolio(value: unknown): value is SavedPortfolio {
  return (
    isObject(value) &&
    typeof value.id === 'string' &&
    typeof value.name === 'string' &&
    value.currency === 'BRL' &&
    Number.isSafeInteger(value.version) &&
    typeof value.createdAt === 'string' &&
    typeof value.updatedAt === 'string' &&
    Array.isArray(value.targets) &&
    value.targets.length <= 20 &&
    value.targets.every(
      (target: unknown) =>
        isObject(target) &&
        typeof target.id === 'string' &&
        typeof target.name === 'string' &&
        typeof target.targetPercentage === 'string' &&
        Number.isInteger(target.displayOrder),
    )
  )
}

async function request(
  path: string,
  options: RequestInit = {},
): Promise<unknown> {
  const controller = new AbortController()
  let timedOut = false
  const cancel = () => controller.abort()
  options.signal?.addEventListener('abort', cancel, { once: true })
  if (options.signal?.aborted) controller.abort()
  const timeout = setTimeout(() => {
    timedOut = true
    controller.abort()
  }, 30000)
  try {
    const response = await fetch(path, {
      ...options,
      signal: controller.signal,
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { Accept: 'application/json', ...options.headers },
    })
    if (controller.signal.aborted)
      throw new DOMException('Request cancelled', 'AbortError')
    if (!response.ok) {
      const messages: Record<number, string> = {
        400: 'Confira os dados informados e tente novamente.',
        401: 'Sua sessão expirou. Entre novamente para continuar.',
        403: 'Não foi possível autorizar esta operação. Verifique sua sessão e o acesso à carteira.',
        404: 'A carteira não está disponível para esta sessão.',
        409: 'A carteira foi atualizada em outra sessão. Seu rascunho foi preservado.',
      }
      throw new WalletApiError(
        response.status,
        messages[response.status] ??
          'Não foi possível concluir a operação. Tente novamente.',
      )
    }
    if (response.status === 204) return null
    return await response.json()
  } catch (error) {
    if (options.signal?.aborted)
      throw new DOMException('Request cancelled', 'AbortError')
    if (timedOut)
      throw new WalletApiError(
        0,
        'A API excedeu o tempo de espera de 30 segundos. Tente novamente.',
      )
    if (error instanceof WalletApiError) throw error
    throw new WalletApiError(
      0,
      'Não foi possível conectar à API. Confira se o serviço está disponível.',
    )
  } finally {
    clearTimeout(timeout)
    options.signal?.removeEventListener('abort', cancel)
  }
}

function invalidResponse(): never {
  throw new WalletApiError(
    0,
    'A API retornou uma resposta inesperada. Recarregue e tente novamente.',
  )
}

async function mutate(
  path: string,
  method: 'POST' | 'PUT',
  signal: AbortSignal,
  body?: unknown,
) {
  const csrf = await request('/api/v1/csrf', { signal })
  if (
    !isObject(csrf) ||
    typeof csrf.headerName !== 'string' ||
    typeof csrf.token !== 'string'
  ) {
    return invalidResponse()
  }
  return request(path, {
    method,
    signal,
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}

export async function readSession(signal: AbortSignal): Promise<WalletSession> {
  const value = await request('/api/v1/session', { signal })
  if (
    !isObject(value) ||
    typeof value.authenticated !== 'boolean' ||
    typeof value.walletAvailable !== 'boolean' ||
    !['demo', 'local', 'google'].includes(String(value.loginMode)) ||
    (value.displayName != null && typeof value.displayName !== 'string')
  )
    return invalidResponse()
  return value as unknown as WalletSession
}

export async function readPortfolios(
  signal: AbortSignal,
): Promise<SavedPortfolio[]> {
  const value = await request('/api/v1/portfolios', { signal })
  if (
    !isObject(value) ||
    !Array.isArray(value.portfolios) ||
    value.portfolios.length > 100 ||
    !value.portfolios.every(isPortfolio)
  ) {
    return invalidResponse()
  }
  return value.portfolios
}

export async function startLocalSession(signal: AbortSignal): Promise<void> {
  await mutate('/api/v1/local-session', 'POST', signal)
}

export async function endSession(signal: AbortSignal): Promise<void> {
  await mutate('/api/v1/logout', 'POST', signal)
}

export async function createPortfolio(
  name: string,
  signal: AbortSignal,
): Promise<SavedPortfolio> {
  const value = await mutate('/api/v1/portfolios', 'POST', signal, {
    name,
    currency: 'BRL',
  })
  if (!isPortfolio(value)) return invalidResponse()
  return value
}

export async function saveTargets(
  id: string,
  body: SaveTargetsRequest,
  signal: AbortSignal,
): Promise<SavedPortfolio> {
  const value = await mutate(
    `/api/v1/portfolios/${encodeURIComponent(id)}/targets`,
    'PUT',
    signal,
    body,
  )
  if (!isPortfolio(value)) return invalidResponse()
  return value
}
