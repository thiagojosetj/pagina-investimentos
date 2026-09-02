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
    })
  } catch {
    throw new SimulationApiError(
      'Não foi possível conectar à API. Confirme se o backend está em execução.',
    )
  }

  if (!response.ok) {
    const problem = await readProblem(response)
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
