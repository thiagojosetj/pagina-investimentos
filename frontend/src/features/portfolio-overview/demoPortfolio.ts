export interface DemoCategory {
  id: string
  name: string
  currentPercentage: string
  targetPercentage: string
  marketValue: string
  deviationLabel: string
  color: string
}

export interface DemoPortfolioItem {
  id: string
  code: string
  name: string
  type: string
  categoryId: string
  marketValue: string
  allocationPercentage: string
}

interface DemoPortfolio {
  referenceDate: string
  totalBalance: string
  positionsValue: string
  cashValue: string
  categories: readonly DemoCategory[]
  items: readonly DemoPortfolioItem[]
}

export const DEMO_PORTFOLIO: DemoPortfolio = {
  referenceDate: '02/09/2026',
  totalBalance: '60000.00',
  positionsValue: '57000.00',
  cashValue: '3000.00',
  categories: [
    {
      id: 'stocks',
      name: 'Ações',
      currentPercentage: '40.00',
      targetPercentage: '35.00',
      marketValue: '24000.00',
      deviationLabel: '+5,00 p.p.',
      color: '#087e71',
    },
    {
      id: 'real-estate-funds',
      name: 'FIIs',
      currentPercentage: '20.00',
      targetPercentage: '25.00',
      marketValue: '12000.00',
      deviationLabel: '−5,00 p.p.',
      color: '#d89b36',
    },
    {
      id: 'etfs',
      name: 'ETFs',
      currentPercentage: '15.00',
      targetPercentage: '15.00',
      marketValue: '9000.00',
      deviationLabel: 'na meta',
      color: '#5974a9',
    },
    {
      id: 'fixed-income',
      name: 'Renda fixa',
      currentPercentage: '20.00',
      targetPercentage: '20.00',
      marketValue: '12000.00',
      deviationLabel: 'na meta',
      color: '#a2647e',
    },
    {
      id: 'cash',
      name: 'Caixa',
      currentPercentage: '5.00',
      targetPercentage: '5.00',
      marketValue: '3000.00',
      deviationLabel: 'na meta',
      color: '#688a4e',
    },
  ],
  items: [
    {
      id: 'demo-stock-one',
      code: 'SYN-STK-01',
      name: 'Empresa Horizonte — exemplo',
      type: 'Ação fictícia',
      categoryId: 'stocks',
      marketValue: '14000.00',
      allocationPercentage: '23.33',
    },
    {
      id: 'demo-stock-two',
      code: 'SYN-STK-02',
      name: 'Indústria Aurora — exemplo',
      type: 'Ação fictícia',
      categoryId: 'stocks',
      marketValue: '10000.00',
      allocationPercentage: '16.67',
    },
    {
      id: 'demo-fii-one',
      code: 'SYN-FII-01',
      name: 'Fundo Mosaico — exemplo',
      type: 'FII fictício',
      categoryId: 'real-estate-funds',
      marketValue: '7000.00',
      allocationPercentage: '11.67',
    },
    {
      id: 'demo-fii-two',
      code: 'SYN-FII-02',
      name: 'Fundo Vértice — exemplo',
      type: 'FII fictício',
      categoryId: 'real-estate-funds',
      marketValue: '5000.00',
      allocationPercentage: '8.33',
    },
    {
      id: 'demo-etf',
      code: 'SYN-ETF-01',
      name: 'Índice Global — exemplo',
      type: 'ETF fictício',
      categoryId: 'etfs',
      marketValue: '9000.00',
      allocationPercentage: '15.00',
    },
    {
      id: 'demo-public-bond',
      code: 'SYN-RF-01',
      name: 'Título público — exemplo',
      type: 'Título público fictício',
      categoryId: 'fixed-income',
      marketValue: '7000.00',
      allocationPercentage: '11.67',
    },
    {
      id: 'demo-bank-bond',
      code: 'SYN-CDB-01',
      name: 'CDB Banco Modelo — exemplo',
      type: 'CDB fictício',
      categoryId: 'fixed-income',
      marketValue: '5000.00',
      allocationPercentage: '8.33',
    },
    {
      id: 'demo-cash',
      code: 'SYN-CASH',
      name: 'Caixa disponível — exemplo',
      type: 'Caixa fictício',
      categoryId: 'cash',
      marketValue: '3000.00',
      allocationPercentage: '5.00',
    },
  ],
}
