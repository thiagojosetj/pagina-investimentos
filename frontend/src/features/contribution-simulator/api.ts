import type {
  ContributionSimulationRequest,
  ContributionSimulationResponse,
  ProblemDetail,
} from './contracts'

export class SimulationApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'SimulationApiError'
  }
}

export async function simulateContribution(
  request: ContributionSimulationRequest,
  signal?: AbortSignal,
): Promise<ContributionSimulationResponse> {
  const controller = new AbortController()
  let timedOut = false
  const cancelRequest = () => controller.abort()
  signal?.addEventListener('abort', cancelRequest, { once: true })
  if (signal?.aborted) controller.abort()
  const timeout = setTimeout(() => {
    timedOut = true
    controller.abort()
  }, 120000)

  try {
    const response = await fetch(
      '/api/v1/allocation-simulations/contributions',
      {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
        signal: controller.signal,
      },
    )

    if (!response.ok) {
      const problem = await readProblem(response)
      if (
        !problem.detail &&
        !problem.title &&
        [502, 503, 504].includes(response.status)
      ) {
        throw new SimulationApiError(
          'A API está temporariamente indisponível. Aguarde um momento e tente novamente.',
        )
      }
      throw new SimulationApiError(
        problem.detail ??
          problem.title ??
          'Não foi possível concluir a simulação.',
      )
    }

    return (await response.json()) as ContributionSimulationResponse
  } catch (error) {
    if (timedOut) {
      throw new SimulationApiError(
        'A simulação excedeu o tempo de espera de dois minutos. Tente novamente.',
      )
    }
    if (controller.signal.aborted) {
      throw new DOMException('Simulation cancelled', 'AbortError')
    }
    if (error instanceof SimulationApiError) throw error
    if (
      (error instanceof Error || error instanceof DOMException) &&
      error.name === 'AbortError'
    ) {
      throw error
    }
    throw new SimulationApiError(
      'Não foi possível conectar à API. Aguarde um momento e tente novamente.',
    )
  } finally {
    clearTimeout(timeout)
    signal?.removeEventListener('abort', cancelRequest)
  }
}

async function readProblem(response: Response): Promise<ProblemDetail> {
  try {
    return (await response.json()) as ProblemDetail
  } catch (error) {
    if (
      (error instanceof Error || error instanceof DOMException) &&
      error.name === 'AbortError'
    )
      throw error
    return {}
  }
}
