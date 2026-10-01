import { describe, expect, it } from 'vitest'
import { targetsRequest, validateTargets } from './contracts'
import type { SavedPortfolio, TargetDraft } from './contracts'

function draft(percentages: string[]): TargetDraft[] {
  return percentages.map((percentage, index) => ({
    key: String(index),
    id: null,
    name: `Classe ${index + 1}`,
    percentage,
  }))
}

describe('saved allocation target validation', () => {
  it('serializes leading zeros and comma values as canonical four-decimal strings', () => {
    const portfolio = { version: 3 } as SavedPortfolio
    const result = targetsRequest(portfolio, draft(['040', '060,0000']))
    expect(result.targets.map((target) => target.targetPercentage)).toEqual([
      '40.0000',
      '60.0000',
    ])
    expect(result.expectedVersion).toBe(3)
  })

  it('accepts a precise four-decimal sum and zero without binary rounding', () => {
    expect(
      validateTargets(draft(['33,3333', '33.3333', '33,3334', '0'])),
    ).toBeNull()
  })

  it.each(['99,9999', '100,0001', '-1', '1e2', '100,00000', ''])(
    'rejects an invalid or non-exact percentage %s',
    (percentage) => {
      expect(validateTargets(draft([percentage]))).not.toBeNull()
    },
  )

  it('rejects duplicate class names and excess classes', () => {
    const values = draft(['40', '60'])
    values[0]!.name = 'FIIs'
    values[1]!.name = ' fiis '
    expect(validateTargets(values)).toContain('nomes diferentes')
    expect(
      validateTargets(draft(Array.from({ length: 21 }, () => '0'))),
    ).toContain('20 classes')
  })
})
