export interface WalletSession {
  authenticated: boolean
  displayName?: string | null
  loginMode: 'demo' | 'local' | 'google'
  walletAvailable: boolean
}

export interface PortfolioTarget {
  id: string
  name: string
  targetPercentage: string
  displayOrder: number
}

export interface SavedPortfolio {
  id: string
  name: string
  currency: 'BRL'
  version: number
  createdAt: string
  updatedAt: string
  targets: PortfolioTarget[]
}

export interface TargetDraft {
  key: string
  id: string | null
  name: string
  percentage: string
}

export interface SaveTargetsRequest {
  expectedVersion: number
  targets: Array<{
    id: string | null
    name: string
    targetPercentage: string
    displayOrder: number
  }>
}

export function createTargetDraft(portfolio: SavedPortfolio): TargetDraft[] {
  return portfolio.targets
    .toSorted((first, second) => first.displayOrder - second.displayOrder)
    .map((target) => ({
      key: target.id,
      id: target.id,
      name: target.name,
      percentage: target.targetPercentage.replace('.', ','),
    }))
}

function percentageUnits(value: string): bigint | null {
  const match = /^(\d{1,3})(?:[,.](\d{1,4}))?$/.exec(value.trim())
  if (!match) return null
  const units =
    BigInt(match[1]!) * 10000n + BigInt((match[2] ?? '').padEnd(4, '0'))
  return units <= 1000000n ? units : null
}

export function validateTargets(draft: TargetDraft[]): string | null {
  if (draft.length < 1 || draft.length > 20) {
    return 'Defina entre 1 e 20 classes para esta carteira.'
  }
  const names = new Set<string>()
  let total = 0n
  for (const target of draft) {
    const name = target.name.trim()
    if (!name || name.length > 60) {
      return 'Cada classe precisa de um nome entre 1 e 60 caracteres.'
    }
    const normalizedName = name.toLocaleLowerCase('pt-BR')
    if (names.has(normalizedName))
      return 'Use nomes diferentes para as classes.'
    names.add(normalizedName)
    const units = percentageUnits(target.percentage)
    if (units === null) {
      return 'Informe metas entre 0 e 100%, com até quatro casas decimais.'
    }
    total += units
  }
  return total === 1000000n
    ? null
    : 'A soma das metas deve ser exatamente 100%, sem tolerância.'
}

export function targetsRequest(
  portfolio: SavedPortfolio,
  draft: TargetDraft[],
): SaveTargetsRequest {
  const error = validateTargets(draft)
  if (error) throw new Error(error)
  return {
    expectedVersion: portfolio.version,
    targets: draft.map((target, displayOrder) => {
      const units = percentageUnits(target.percentage)!
      return {
        id: target.id,
        name: target.name.trim(),
        targetPercentage: `${units / 10000n}.${(units % 10000n).toString().padStart(4, '0')}`,
        displayOrder,
      }
    }),
  }
}
