import { useMemo, useState } from 'react'
import { DEMO_PORTFOLIO } from './demoPortfolio'
import './PortfolioOverview.css'

interface PortfolioOverviewProps {
  onOpenSimulator: () => void
}

function formatCurrency(value: string): string {
  const [integerPart, decimalPart = '00'] = value.split('.')
  const groupedInteger = BigInt(integerPart).toLocaleString('pt-BR')
  return `R$ ${groupedInteger},${decimalPart.padEnd(2, '0').slice(0, 2)}`
}

export function PortfolioOverview({ onOpenSimulator }: PortfolioOverviewProps) {
  const [selectedCategory, setSelectedCategory] = useState('all')
  const visibleItems = useMemo(
    () =>
      selectedCategory === 'all'
        ? DEMO_PORTFOLIO.items
        : DEMO_PORTFOLIO.items.filter(
            (item) => item.categoryId === selectedCategory,
          ),
    [selectedCategory],
  )

  return (
    <section className="portfolio-overview" aria-labelledby="overview-title">
      <div className="overview-heading">
        <div>
          <p className="eyebrow">Visão geral · demonstração</p>
          <h1 id="overview-title">Sua carteira, organizada em um só lugar.</h1>
          <p>
            Esta tela antecipa a experiência principal do produto com uma
            carteira inteiramente sintética. Nenhum valor é salvo ou consultado
            em um provedor de mercado.
          </p>
        </div>
        <div className="synthetic-source" role="note">
          <span>Fonte</span>
          <strong>Fixture sintética</strong>
          <small>Referência fixa: {DEMO_PORTFOLIO.referenceDate}</small>
        </div>
      </div>

      <div className="overview-totals" aria-label="Resumo do cenário sintético">
        <article className="primary-total">
          <span>Total do cenário sintético</span>
          <strong>{formatCurrency(DEMO_PORTFOLIO.totalBalance)}</strong>
          <small>Itens e hipótese de caixa</small>
        </article>
        <article>
          <span>Valor sintético das posições</span>
          <strong>{formatCurrency(DEMO_PORTFOLIO.positionsValue)}</strong>
          <small>Sem cotação ou rentabilidade calculada</small>
        </article>
        <article>
          <span>Hipótese visual de caixa</span>
          <strong>{formatCurrency(DEMO_PORTFOLIO.cashValue)}</strong>
          <small>Ainda não é um modelo persistido</small>
        </article>
      </div>

      <div className="overview-grid">
        <article className="overview-panel allocation-panel">
          <div className="overview-panel-heading">
            <div>
              <p className="section-index">01 / Composição</p>
              <h2>Alocação por categoria</h2>
            </div>
            <span>Atual × meta</span>
          </div>

          <div
            className="overview-allocation-strip"
            role="img"
            aria-label="Composição sintética da carteira por categoria"
          >
            {DEMO_PORTFOLIO.categories.map((category) => (
              <span
                key={category.id}
                style={{
                  backgroundColor: category.color,
                  width: `${category.currentPercentage}%`,
                }}
                title={`${category.name}: ${category.currentPercentage}%`}
              />
            ))}
          </div>

          <div className="category-list">
            {DEMO_PORTFOLIO.categories.map((category) => (
              <div className="category-row" key={category.id}>
                <span
                  className="overview-color-key"
                  style={{ backgroundColor: category.color }}
                  aria-hidden="true"
                />
                <div>
                  <strong>{category.name}</strong>
                  <small>{formatCurrency(category.marketValue)}</small>
                </div>
                <div className="category-percentages">
                  <strong>{category.currentPercentage}%</strong>
                  <small>meta {category.targetPercentage}%</small>
                </div>
                <span
                  className={
                    category.deviationLabel === 'na meta'
                      ? 'deviation on-target'
                      : 'deviation'
                  }
                >
                  {category.deviationLabel}
                </span>
              </div>
            ))}
          </div>

          <div className="allocation-action">
            <div>
              <strong>Quer explorar um novo aporte?</strong>
              <span>
                Use suas próprias metas em uma simulação separada desta
                demonstração.
              </span>
            </div>
            <button type="button" onClick={onOpenSimulator}>
              Abrir simulador
              <span aria-hidden="true">→</span>
            </button>
          </div>
        </article>

        <article className="overview-panel holdings-panel">
          <div className="overview-panel-heading holdings-heading">
            <div>
              <p className="section-index">02 / Itens</p>
              <h2 id="holdings-title">Posições demonstrativas</h2>
            </div>
            <span>{visibleItems.length} itens</span>
          </div>

          <div
            className="category-filters"
            role="group"
            aria-label="Filtrar por categoria"
          >
            <button
              className={selectedCategory === 'all' ? 'active' : undefined}
              type="button"
              onClick={() => setSelectedCategory('all')}
              aria-pressed={selectedCategory === 'all'}
            >
              Todos
            </button>
            {DEMO_PORTFOLIO.categories.map((category) => (
              <button
                className={
                  selectedCategory === category.id ? 'active' : undefined
                }
                key={category.id}
                type="button"
                onClick={() => setSelectedCategory(category.id)}
                aria-pressed={selectedCategory === category.id}
              >
                {category.name}
              </button>
            ))}
          </div>

          <div
            aria-labelledby="holdings-title"
            className="holdings-table"
            role="table"
          >
            <div className="holdings-table-header" role="row">
              <span role="columnheader">Ativo</span>
              <span role="columnheader">Tipo</span>
              <span role="columnheader">Valor</span>
              <span role="columnheader">Alocação</span>
            </div>
            {visibleItems.map((item) => (
              <div className="holding-row" role="row" key={item.id}>
                <div role="cell">
                  <strong>{item.code}</strong>
                  <small>{item.name}</small>
                </div>
                <span role="cell">
                  <b className="mobile-cell-label">Tipo</b>
                  {item.type}
                </span>
                <strong role="cell">
                  <b className="mobile-cell-label">Valor sintético</b>
                  {formatCurrency(item.marketValue)}
                </strong>
                <span role="cell">
                  <b className="mobile-cell-label">Alocação</b>
                  {item.allocationPercentage}%
                </span>
              </div>
            ))}
          </div>

          <p className="overview-disclaimer">
            Demonstração visual sem persistência, cotações, rentabilidade ou
            dados financeiros reais. Caixa é apenas uma hipótese de interface,
            não um modelo aprovado.
          </p>
        </article>
      </div>
    </section>
  )
}
