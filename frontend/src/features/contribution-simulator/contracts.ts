export interface AllocationInput {
  classId: string
  name: string
  currentAmount: string
  targetPercentage: string
}

export interface ContributionSimulationRequest {
  currency: 'BRL'
  contribution: string
  allocations: AllocationInput[]
}

export interface AllocationResult {
  classId: string
  name: string
  currentAmount: string
  currentPercentage: string
  targetPercentage: string
  targetAmount: string
  monetaryDeficit: string
  suggestedContribution: string
  projectedAmount: string
  projectedPercentage: string
}

export interface ContributionSimulationResponse {
  method: string
  currency: 'BRL'
  currentTotal: string
  contribution: string
  projectedTotal: string
  allocations: AllocationResult[]
  disclaimer: string
}

export interface ProblemDetail {
  title?: string
  detail?: string
  code?: string
}
