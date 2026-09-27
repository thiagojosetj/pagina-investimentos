import { afterEach, describe, expect, it, vi } from 'vitest'
import { simulateContribution } from './api'
import type { ContributionSimulationRequest } from './contracts'

const request: ContributionSimulationRequest = {
  currency: 'BRL',
  contribution: '100.00',
  includeSales: false,
  allocations: [
    {
      classId: 'stocks',
      name: 'Ações',
      currentAmount: '100.00',
      targetPercentage: '100',
    },
  ],
}

function hangingFetch() {
  const fetchMock = vi.fn().mockImplementation(
    (_url: string, options: RequestInit) =>
      new Promise((_resolve, reject) => {
        if (options.signal?.aborted)
          reject(new DOMException('Cancelled', 'AbortError'))
        else
          options.signal?.addEventListener(
            'abort',
            () => reject(new DOMException('Cancelled', 'AbortError')),
            { once: true },
          )
      }),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('simulation request lifecycle', () => {
  it('allows a slow cold start, then ends a hung request at two minutes', async () => {
    vi.useFakeTimers()
    const fetchMock = hangingFetch()
    const simulation = simulateContribution(request)
    const failure = expect(simulation).rejects.toThrow(
      'A simulação excedeu o tempo de espera de dois minutos. Tente novamente.',
    )
    const signal = (fetchMock.mock.calls[0][1] as RequestInit).signal!
    await vi.advanceTimersByTimeAsync(81000)
    expect(signal.aborted).toBe(false)
    await vi.advanceTimersByTimeAsync(39000)
    await failure
    expect(signal.aborted).toBe(true)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('preserves explicit cancellation and removes its timeout', async () => {
    vi.useFakeTimers()
    const fetchMock = hangingFetch()
    const external = new AbortController()
    const simulation = simulateContribution(request, external.signal)
    const failure = expect(simulation).rejects.toMatchObject({
      name: 'AbortError',
    })
    external.abort()
    await failure
    expect((fetchMock.mock.calls[0][1] as RequestInit).signal?.aborted).toBe(
      true,
    )
    expect(vi.getTimerCount()).toBe(0)
  })

  it('clears the timeout after a successful response', async () => {
    vi.useFakeTimers()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ projectedTotal: '200.00' }), {
          status: 200,
        }),
      ),
    )
    await expect(simulateContribution(request)).resolves.toMatchObject({
      projectedTotal: '200.00',
    })
    expect(vi.getTimerCount()).toBe(0)
  })
})
