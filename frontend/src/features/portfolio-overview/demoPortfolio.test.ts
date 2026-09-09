import { describe, expect, it } from 'vitest'
import { DEMO_PORTFOLIO } from './demoPortfolio'

function decimalToScaledInteger(value: string, scale: number): bigint {
  const [integerPart, decimalPart = ''] = value.split('.')
  return BigInt(
    `${integerPart}${decimalPart.padEnd(scale, '0').slice(0, scale)}`,
  )
}

describe('DEMO_PORTFOLIO', () => {
  it('reconciles its synthetic totals, percentages and references', () => {
    const categoryIds = new Set(
      DEMO_PORTFOLIO.categories.map((category) => category.id),
    )
    const itemIds = new Set(DEMO_PORTFOLIO.items.map((item) => item.id))
    const categoryTotal = DEMO_PORTFOLIO.categories.reduce(
      (total, category) =>
        total + decimalToScaledInteger(category.marketValue, 2),
      0n,
    )
    const itemTotal = DEMO_PORTFOLIO.items.reduce(
      (total, item) => total + decimalToScaledInteger(item.marketValue, 2),
      0n,
    )
    const currentPercentageTotal = DEMO_PORTFOLIO.categories.reduce(
      (total, category) =>
        total + decimalToScaledInteger(category.currentPercentage, 2),
      0n,
    )
    const targetPercentageTotal = DEMO_PORTFOLIO.categories.reduce(
      (total, category) =>
        total + decimalToScaledInteger(category.targetPercentage, 2),
      0n,
    )
    const itemPercentageTotal = DEMO_PORTFOLIO.items.reduce(
      (total, item) =>
        total + decimalToScaledInteger(item.allocationPercentage, 2),
      0n,
    )

    expect(categoryTotal).toBe(
      decimalToScaledInteger(DEMO_PORTFOLIO.totalBalance, 2),
    )
    expect(itemTotal).toBe(
      decimalToScaledInteger(DEMO_PORTFOLIO.totalBalance, 2),
    )
    expect(
      decimalToScaledInteger(DEMO_PORTFOLIO.positionsValue, 2) +
        decimalToScaledInteger(DEMO_PORTFOLIO.cashValue, 2),
    ).toBe(decimalToScaledInteger(DEMO_PORTFOLIO.totalBalance, 2))
    expect(
      DEMO_PORTFOLIO.items
        .filter((item) => item.categoryId !== 'cash')
        .reduce(
          (total, item) => total + decimalToScaledInteger(item.marketValue, 2),
          0n,
        ),
    ).toBe(decimalToScaledInteger(DEMO_PORTFOLIO.positionsValue, 2))
    expect(
      DEMO_PORTFOLIO.items
        .filter((item) => item.categoryId === 'cash')
        .reduce(
          (total, item) => total + decimalToScaledInteger(item.marketValue, 2),
          0n,
        ),
    ).toBe(decimalToScaledInteger(DEMO_PORTFOLIO.cashValue, 2))
    for (const category of DEMO_PORTFOLIO.categories) {
      const itemsInCategory = DEMO_PORTFOLIO.items
        .filter((item) => item.categoryId === category.id)
        .reduce(
          (total, item) => total + decimalToScaledInteger(item.marketValue, 2),
          0n,
        )

      expect(itemsInCategory).toBe(
        decimalToScaledInteger(category.marketValue, 2),
      )
    }
    expect(currentPercentageTotal).toBe(10000n)
    expect(targetPercentageTotal).toBe(10000n)
    expect(itemPercentageTotal).toBe(10000n)
    expect(categoryIds.size).toBe(DEMO_PORTFOLIO.categories.length)
    expect(itemIds.size).toBe(DEMO_PORTFOLIO.items.length)
    expect(
      DEMO_PORTFOLIO.items.every((item) => categoryIds.has(item.categoryId)),
    ).toBe(true)
  })
})
