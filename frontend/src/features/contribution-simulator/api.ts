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
  let response: Response

  try {
    response = await fetch('/api/v1/allocation-simulations/contributions', {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      signal,
    })
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      throw error
    }
    throw new SimulationApiError(
      'Não foi possível conectar à API. Aguarde um momento e tente novamente.',
    )
  }

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
}

async function readProblem(response: Response): Promise<ProblemDetail> {
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return {}
  }
}
