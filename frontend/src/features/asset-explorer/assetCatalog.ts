import {
  DEMO_PORTFOLIO,
  type DemoPortfolioItem,
} from '../portfolio-overview/demoPortfolio'

export type AssetSort = 'code' | 'name'

export const DEMO_ASSET_CATEGORIES = DEMO_PORTFOLIO.categories.filter(
  (category) => category.id !== 'cash',
)

export const DEMO_ASSETS = DEMO_PORTFOLIO.items.filter(
  (item) => item.categoryId !== 'cash',
)

export function normalizeAssetSearch(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR')
    .replace(/\s+/g, '')
}

export function filterDemoAssets(
  search: string,
  categoryId: string,
  sort: AssetSort,
): DemoPortfolioItem[] {
  const normalizedSearch = normalizeAssetSearch(search)
  return DEMO_ASSETS.filter((item) => {
    const category = DEMO_ASSET_CATEGORIES.find(
      (entry) => entry.id === item.categoryId,
    )
    const searchableText = [item.code, item.name, item.type, category?.name]
      .map((part) => normalizeAssetSearch(part ?? ''))
      .join(' ')
    return (
      (categoryId === 'all' || item.categoryId === categoryId) &&
      searchableText.includes(normalizedSearch)
    )
  }).sort((left, right) =>
    left[sort].localeCompare(right[sort], 'pt-BR', {
      sensitivity: 'base',
      numeric: true,
    }),
  )
}

// Format only: no monetary calculation or conversion to binary floating point.
export function formatAssetCurrency(value: string): string {
  if (!/^\d+(?:\.\d{1,2})?$/.test(value)) {
    throw new Error('Valor monetário deve ter no máximo duas casas decimais.')
  }
  const [integerPart, decimalPart = '00'] = value.split('.')
  return `R$ ${BigInt(integerPart).toLocaleString('pt-BR')},${decimalPart.padEnd(2, '0')}`
}

export function formatAssetPercentage(value: string): string {
  return `${value.replace('.', ',')}%`
}
