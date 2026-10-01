import { describe, expect, it } from 'vitest'
import {
  DEMO_ASSETS,
  DEMO_ASSET_CATEGORIES,
  filterDemoAssets,
  formatAssetCurrency,
  formatAssetPercentage,
  normalizeAssetSearch,
} from './assetCatalog'

describe('asset explorer presentation helpers', () => {
  it('uses the existing seven synthetic investments and excludes hypothetical cash', () => {
    expect(DEMO_ASSETS).toHaveLength(7)
    expect(DEMO_ASSET_CATEGORIES).toHaveLength(4)
    expect(DEMO_ASSETS.every((item) => item.categoryId !== 'cash')).toBe(true)
    expect(
      DEMO_ASSET_CATEGORIES.some((category) => category.id === 'cash'),
    ).toBe(false)
  })

  it('normalizes accents, combining Unicode, case and whitespace', () => {
    expect(normalizeAssetSearch('  ÍNDICE \n Global  ')).toBe('indiceglobal')
    expect(
      filterDemoAssets(' V E R T I C E ', 'all', 'code').map(
        (asset) => asset.code,
      ),
    ).toEqual(['SYN-FII-02'])
    expect(
      filterDemoAssets('ÍNDICE', 'all', 'code').map((asset) => asset.code),
    ).toEqual(['SYN-ETF-01'])
  })

  it('searches code, type and category and combines search with the category filter', () => {
    expect(filterDemoAssets('syn-fii', 'all', 'code')).toHaveLength(2)
    expect(filterDemoAssets('Acao Ficticia', 'all', 'code')).toHaveLength(2)
    expect(filterDemoAssets(' RENDA FIXA ', 'all', 'code')).toHaveLength(2)
    expect(filterDemoAssets('fundo', 'real-estate-funds', 'code')).toHaveLength(
      2,
    )
    expect(filterDemoAssets('fundo', 'stocks', 'code')).toEqual([])
  })

  it('orders by code or name without changing the shared fixture', () => {
    const initialOrder = DEMO_ASSETS.map((asset) => asset.code)
    expect(filterDemoAssets('', 'all', 'code')[0].code).toBe('SYN-CDB-01')
    expect(filterDemoAssets('', 'all', 'code')[1].code).toBe('SYN-ETF-01')
    expect(filterDemoAssets('', 'all', 'name')[0].name).toBe(
      'CDB Banco Modelo — exemplo',
    )
    expect(filterDemoAssets('', 'all', 'name')[1].code).toBe('SYN-STK-01')
    expect(filterDemoAssets('', 'stocks', 'name')[0].code).toBe('SYN-STK-01')
    expect(DEMO_ASSETS.map((asset) => asset.code)).toEqual(initialOrder)
  })

  it('formats cents and large integers without floating point or truncation', () => {
    expect(formatAssetCurrency('0')).toBe('R$ 0,00')
    expect(formatAssetCurrency('7000.5')).toBe('R$ 7.000,50')
    expect(formatAssetCurrency('9007199254740993.01')).toBe(
      'R$ 9.007.199.254.740.993,01',
    )
    expect(formatAssetPercentage('11.67')).toBe('11,67%')
  })

  it.each(['1.001', 'bad', '1,00', '-1.00'])(
    'rejects unsupported currency input %s instead of silently rounding',
    (value) => {
      expect(() => formatAssetCurrency(value)).toThrow(/duas casas decimais/)
    },
  )
})
