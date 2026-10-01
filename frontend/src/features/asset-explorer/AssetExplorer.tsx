import { useLayoutEffect, useMemo, useRef, useState } from 'react'
import { DEMO_PORTFOLIO } from '../portfolio-overview/demoPortfolio'
import {
  DEMO_ASSETS,
  DEMO_ASSET_CATEGORIES,
  filterDemoAssets,
  formatAssetCurrency,
  formatAssetPercentage,
  type AssetSort,
} from './assetCatalog'
import './AssetExplorer.css'

interface AssetExplorerProps {
  initialCategoryId?: string
}

export function AssetExplorer({
  initialCategoryId = 'all',
}: AssetExplorerProps) {
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState(initialCategoryId)
  const [sort, setSort] = useState<AssetSort>('code')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const detailButtons = useRef(new Map<string, HTMLButtonElement>())
  const searchInput = useRef<HTMLInputElement>(null)
  const detailsHeading = useRef<HTMLHeadingElement>(null)
  const visibleAssets = useMemo(
    () => filterDemoAssets(search, categoryId, sort),
    [search, categoryId, sort],
  )
  const selectedAsset = visibleAssets.find((asset) => asset.id === selectedId)
  const selectedCategory = DEMO_ASSET_CATEGORIES.find(
    (category) => category.id === selectedAsset?.categoryId,
  )

  useLayoutEffect(() => {
    if (!selectedAsset) return
    const heading = detailsHeading.current
    heading?.focus({ preventScroll: true })
    const allowMotion = window.matchMedia?.(
      '(prefers-reduced-motion: no-preference)',
    ).matches
    heading?.scrollIntoView?.({
      block: 'nearest',
      behavior: allowMotion ? 'smooth' : 'auto',
    })
  }, [selectedAsset])

  function changeFilters(nextSearch: string, nextCategory: string) {
    setSearch(nextSearch)
    setCategoryId(nextCategory)
    if (
      selectedId &&
      !filterDemoAssets(nextSearch, nextCategory, sort).some(
        (asset) => asset.id === selectedId,
      )
    ) {
      setSelectedId(null)
    }
  }

  function closeDetails() {
    if (selectedId) detailButtons.current.get(selectedId)?.focus()
    setSelectedId(null)
  }

  function clearFilters() {
    changeFilters('', 'all')
    // The reset button disappears; keep keyboard navigation on a stable control.
    searchInput.current?.focus()
  }

  return (
    <section className="assets-explorer" aria-labelledby="assets-title">
      <div className="assets-heading">
        <div>
          <p className="eyebrow">Ativos · demonstração</p>
          <h1 id="assets-title">Explore os ativos da demonstração.</h1>
          <p>
            Encontre um item por nome, código ou categoria e conheça sua
            participação no cenário. Uma nova perspectiva sobre a mesma carteira
            sintética, sem consultar o mercado.
          </p>
        </div>
        <div className="assets-source" role="note">
          <span>Fonte dos dados</span>
          <strong>Fixture sintética</strong>
          <small>Referência fixa: {DEMO_PORTFOLIO.referenceDate}</small>
        </div>
      </div>

      <div className="assets-summary" aria-label="Resumo dos ativos sintéticos">
        <article>
          <span>Universo demonstrativo</span>
          <strong>{DEMO_ASSETS.length} ativos</strong>
          <small>
            {DEMO_ASSET_CATEGORIES.length} categorias de investimentos
          </small>
        </article>
        <article>
          <span>Valor sintético das posições</span>
          <strong>{formatAssetCurrency(DEMO_PORTFOLIO.positionsValue)}</strong>
          <small>Não representa cotações atuais</small>
        </article>
        <article>
          <span>Base das participações</span>
          <strong>{formatAssetCurrency(DEMO_PORTFOLIO.totalBalance)}</strong>
          <small>Inclui a hipótese visual de caixa da visão geral</small>
        </article>
      </div>

      <div className="assets-workspace">
        <div className="assets-list-panel">
          <div className="assets-panel-heading">
            <div>
              <p className="section-index">01 / Explorar</p>
              <h2 id="assets-list-title">Encontre um ativo</h2>
            </div>
            <span className="assets-demo-badge">Dados fictícios</span>
          </div>

          <div className="assets-controls">
            <label className="assets-search">
              <span>Buscar ativos</span>
              <input
                type="search"
                ref={searchInput}
                value={search}
                onChange={(event) =>
                  changeFilters(event.target.value, categoryId)
                }
                placeholder="Nome, código, tipo ou categoria"
              />
            </label>
            <label className="assets-sort">
              <span>Ordenar ativos</span>
              <select
                value={sort}
                onChange={(event) => setSort(event.target.value as AssetSort)}
              >
                <option value="code">Código (A–Z)</option>
                <option value="name">Nome (A–Z)</option>
              </select>
            </label>
          </div>

          <div
            className="assets-filters"
            role="group"
            aria-label="Categorias de ativos"
          >
            <button
              type="button"
              aria-pressed={categoryId === 'all'}
              onClick={() => changeFilters(search, 'all')}
            >
              Todos
            </button>
            {DEMO_ASSET_CATEGORIES.map((category) => (
              <button
                key={category.id}
                type="button"
                aria-pressed={categoryId === category.id}
                onClick={() => changeFilters(search, category.id)}
              >
                <span
                  className="assets-category-dot"
                  style={{ backgroundColor: category.color }}
                  aria-hidden="true"
                />
                {category.name}
              </button>
            ))}
          </div>

          <div className="assets-results-heading">
            <p role="status">
              Mostrando {visibleAssets.length} de {DEMO_ASSETS.length} ativos.
            </p>
            {(search !== '' || categoryId !== 'all') && (
              <button
                className="assets-clear"
                type="button"
                onClick={clearFilters}
              >
                Limpar filtros
              </button>
            )}
          </div>

          {visibleAssets.length === 0 ? (
            <div className="assets-empty">
              <span aria-hidden="true">⌕</span>
              <h3>Nenhum ativo encontrado</h3>
              <p>
                Experimente outro termo ou limpe os filtros. Os dados da
                demonstração continuam disponíveis.
              </p>
            </div>
          ) : (
            <ul className="assets-list" aria-labelledby="assets-list-title">
              {visibleAssets.map((asset) => {
                const category = DEMO_ASSET_CATEGORIES.find(
                  (entry) => entry.id === asset.categoryId,
                )
                return (
                  <li
                    key={asset.id}
                    className="assets-card"
                    data-selected={selectedId === asset.id}
                  >
                    <div className="assets-card-heading">
                      <span className="assets-code">{asset.code}</span>
                      <span className="assets-category">
                        <span
                          className="assets-category-dot"
                          style={{ backgroundColor: category?.color }}
                          aria-hidden="true"
                        />
                        {category?.name}
                      </span>
                    </div>
                    <h3>{asset.name}</h3>
                    <p className="assets-type">{asset.type}</p>
                    <div className="assets-card-values">
                      <div>
                        <span>Valor sintético</span>
                        <strong>
                          {formatAssetCurrency(asset.marketValue)}
                        </strong>
                      </div>
                      <div>
                        <span>No cenário total</span>
                        <strong>
                          {formatAssetPercentage(asset.allocationPercentage)}
                        </strong>
                      </div>
                    </div>
                    <button
                      className="assets-detail-button"
                      type="button"
                      aria-label={`Ver detalhes de ${asset.code}`}
                      aria-controls="assets-details"
                      aria-expanded={selectedId === asset.id}
                      onClick={() => setSelectedId(asset.id)}
                      ref={(button) => {
                        if (button) detailButtons.current.set(asset.id, button)
                        else detailButtons.current.delete(asset.id)
                      }}
                    >
                      Ver detalhes <span aria-hidden="true">↗</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </div>

        <aside
          className="assets-details"
          id="assets-details"
          role="region"
          aria-label="Detalhes do ativo"
        >
          <div className="assets-panel-heading">
            <div>
              <p className="section-index">02 / Em foco</p>
              <h2 ref={detailsHeading} tabIndex={-1}>
                Detalhes do ativo
              </h2>
            </div>
            {selectedAsset && (
              <button
                className="assets-close"
                type="button"
                onClick={closeDetails}
              >
                Fechar detalhes
              </button>
            )}
          </div>
          {selectedAsset ? (
            <div className="assets-detail-content">
              <span className="assets-code">{selectedAsset.code}</span>
              <h3>{selectedAsset.name}</h3>
              <p>{selectedAsset.type}</p>
              <dl>
                <div>
                  <dt>Categoria</dt>
                  <dd>{selectedCategory?.name}</dd>
                </div>
                <div>
                  <dt>Valor sintético</dt>
                  <dd>{formatAssetCurrency(selectedAsset.marketValue)}</dd>
                </div>
                <div>
                  <dt>Participação no cenário total</dt>
                  <dd>
                    {formatAssetPercentage(selectedAsset.allocationPercentage)}
                  </dd>
                </div>
                <div>
                  <dt>Fonte</dt>
                  <dd>Fixture sintética</dd>
                </div>
                <div>
                  <dt>Referência fixa</dt>
                  <dd>{DEMO_PORTFOLIO.referenceDate}</dd>
                </div>
              </dl>
              <p className="assets-detail-note">
                Participação predefinida sobre{' '}
                {formatAssetCurrency(DEMO_PORTFOLIO.totalBalance)}, incluindo a
                hipótese visual de caixa. Não é recalculada ao filtrar a lista.
              </p>
              <p className="assets-detail-note">
                Este exemplo não contém quantidade, preço unitário, custo de
                compra, dividendos ou rentabilidade. Esses dados não são
                inferidos.
              </p>
            </div>
          ) : (
            <div className="assets-detail-invitation">
              <div className="assets-detail-illustration" aria-hidden="true">
                <span />
                <span />
                <span />
              </div>
              <h3>Um ativo, uma visão mais próxima.</h3>
              <p>
                Escolha “Ver detalhes” em um item da lista para consultar suas
                informações sintéticas aqui.
              </p>
            </div>
          )}
          <p className="assets-disclaimer">
            Demonstração sem cadastro, persistência, cotações ou dados pessoais.
            Caixa é apenas uma hipótese da visão geral e não aparece como ativo
            nesta lista. Não constitui recomendação de investimento.
          </p>
        </aside>
      </div>
    </section>
  )
}
