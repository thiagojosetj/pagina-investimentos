import { afterEach, describe, expect, it, vi } from 'vitest'
import { readSession, WalletApiError } from './api'

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('saved portfolio API', () => {
  it('accepts a nullable display name in the anonymous backend contract', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            authenticated: false,
            displayName: null,
            loginMode: 'demo',
            walletAvailable: false,
          }),
        ),
      ),
    )
    expect(await readSession(new AbortController().signal)).toMatchObject({
      authenticated: false,
      displayName: null,
    })
  })

  it('turns a network failure into an actionable error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new TypeError('network failure')),
    )
    await expect(readSession(new AbortController().signal)).rejects.toThrow(
      'Não foi possível conectar à API',
    )
  })

  it('limits a stalled request to 30 seconds', async () => {
    vi.useFakeTimers()
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_path: string, options: RequestInit) =>
          new Promise((_resolve, reject) => {
            options.signal?.addEventListener('abort', () =>
              reject(new DOMException('Aborted', 'AbortError')),
            )
          }),
      ),
    )
    const result = readSession(new AbortController().signal)
    const assertion = expect(result).rejects.toThrow('30 segundos')
    await vi.advanceTimersByTimeAsync(30000)
    await assertion
  })

  it('distinguishes caller cancellation from a connection error', async () => {
    const controller = new AbortController()
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_path: string, options: RequestInit) =>
          new Promise((_resolve, reject) => {
            options.signal?.addEventListener('abort', () =>
              reject(new DOMException('Aborted', 'AbortError')),
            )
          }),
      ),
    )
    const result = readSession(controller.signal)
    controller.abort()
    await expect(result).rejects.toMatchObject({ name: 'AbortError' })
  })

  it('rejects a malformed session response instead of rendering a false login state', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify({ authenticated: true })),
        ),
    )
    await expect(
      readSession(new AbortController().signal),
    ).rejects.toBeInstanceOf(WalletApiError)
  })
})
