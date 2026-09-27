import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { simulateContribution } from './api'
import type {
  AllocationResult,
  ContributionSimulationRequest,
  ContributionSimulationResponse,
  SimulationDraftPreset,
} from './contracts'
import './ContributionSimulator.css'

interface DraftAllocation {
  classId: string
  name: string
  currentAmount: string
  targetPercentage: string
}

const INITIAL_ALLOCATIONS: DraftAllocation[] = [
  {
    classId: 'stocks',
    name: 'Ações',
    currentAmount: '4800,00',
    targetPercentage: '40',
  },
  {
    classId: 'real-estate-funds',
    name: 'FIIs',
    currentAmount: '1800,00',
    targetPercentage: '25',
  },
  {
    classId: 'etfs',
    name: 'ETFs',
    currentAmount: '1400,00',
    targetPercentage: '15',
  },
  {
    classId: 'fixed-income',
    name: 'Renda fixa',
    currentAmount: '2000,00',
    targetPercentage: '20',
  },
]

const COLORS = [
  '#087e71',
  '#d89b36',
  '#5974a9',
  '#a2647e',
  '#688a4e',
  '#9a6b3e',
]

const percentageFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
})

function normalizeDecimal(value: string): string {
  const trimmed = value.trim().replace(/\s/g, '')

  if (trimmed.includes(',')) {
    return trimmed.replace(/\./g, '').replace(',', '.')
  }

  return trimmed
}

function completeMoneyValue(value: string): string {
  const normalized = normalizeDecimal(value)

  if (!/^\d+(?:\.\d{0,2})?$/.test(normalized)) {
    return value
  }

  const [integerPart, decimalPart = ''] = normalized.split('.')
  return `${integerPart},${decimalPart.padEnd(2, '0')}`
}

function parseTargetUnits(value: string): bigint | null {
  const normalized = normalizeDecimal(value)
  if (!/^(?:(?:0|[1-9]\d?)(?:\.\d{1,4})?|100(?:\.0{1,4})?)$/.test(normalized)) {
    return null
  }
  const [integerPart, decimalPart = ''] = normalized.split('.')
  return BigInt(integerPart) * 10000n + BigInt(decimalPart.padEnd(4, '0'))
}

function formatTargetTotal(units: bigint): string {
  const integerPart = units / 10000n
  const decimalPart = (units % 10000n).toString().padStart(4, '0')
  return `${integerPart.toLocaleString('pt-BR')},${decimalPart.replace(/0{1,2}$/, '')}%`
}

function formatCurrency(value: string): string {
  const [integerPart, decimalPart = '00'] = value.split('.')
  const groupedInteger = BigInt(integerPart).toLocaleString('pt-BR')
  return `R$ ${groupedInteger},${decimalPart.padEnd(2, '0').slice(0, 2)}`
}

function formatPercentage(value: string): string {
  return `${percentageFormatter.format(Number(value))}%`
}

function barWidth(value: string): string {
  return `${Math.min(100, Math.max(0, Number(value)))}%`
}

function colorForIndex(index: number): string {
  return COLORS[index % COLORS.length]
}

function PortfolioStrip({
  allocations,
  percentageKey,
  label,
}: {
  allocations: AllocationResult[]
  percentageKey: 'currentPercentage' | 'projectedPercentage'
  label: string
}) {
  return (
    <div
      className="portfolio-strip result-portfolio-strip"
      role="img"
      aria-label={label}
    >
      {allocations.map((allocation, index) => (
        <span
          className="portfolio-strip-segment"
          key={allocation.classId}
          style={{
            backgroundColor: colorForIndex(index),
            width: barWidth(allocation[percentageKey]),
            animationDelay: `${180 + index * 50}ms`,
          }}
          title={`${allocation.name}: ${formatPercentage(allocation[percentageKey])}`}
        />
      ))}
    </div>
  )
}

function EmptyResult({ includeSales }: { includeSales: boolean }) {
  return (
    <div className="empty-result">
      <span className="empty-result-number">01</span>
      <div>
        <p className="eyebrow">Como o cálculo funciona</p>
        <h2>
          {includeSales
            ? 'Compras e vendas simuladas aproximam as classes das suas metas.'
            : 'O aporte é simulado a partir dos déficits da carteira.'}
        </h2>
        <p>
          {includeSales
            ? 'O cálculo compara os valores atuais com as metas após o aporte. Os excessos viram vendas hipotéticas, cujo valor é reutilizado nas compras das classes abaixo da meta.'
            : 'O cálculo compara cada valor atual com a meta monetária projetada após o aporte. Classes acima da meta recebem zero; o restante é dividido proporcionalmente entre os déficits.'}
        </p>
        <ul>
          <li>
            {includeSales
              ? 'Valores hipotéticos por classe, sem executar ordens'
              : 'Nenhuma venda é simulada'}
          </li>
          <li>Precisão monetária em centavos</li>
          <li>Resultado determinístico e testável</li>
        </ul>
      </div>
    </div>
  )
}

function SimulationResult({
  result,
}: {
  result: ContributionSimulationResponse
}) {
  return (
    <div className="result-content">
      <div className="result-heading result-stage result-stage-heading">
        <div>
          <p className="eyebrow">Resultado da simulação</p>
          <h2>
            {result.includeSales
              ? 'Equalização por classe'
              : 'Distribuição do novo aporte'}
          </h2>
        </div>
        <span className="method-label">
          {result.includeSales ? 'Vendas incluídas' : 'Déficit proporcional'}
        </span>
      </div>

      <dl className="totals-grid result-stage result-stage-totals">
        <div>
          <dt>Patrimônio atual</dt>
          <dd>{formatCurrency(result.currentTotal)}</dd>
        </div>
        <div className="highlight-total">
          <dt>{result.includeSales ? 'Dinheiro novo' : 'Novo aporte'}</dt>
          <dd>+ {formatCurrency(result.contribution)}</dd>
        </div>
        <div>
          <dt>Total projetado</dt>
          <dd>{formatCurrency(result.projectedTotal)}</dd>
        </div>
      </dl>

      {result.includeSales ? (
        <p className="sales-result-explanation">
          As compras simuladas usam o dinheiro novo e os valores das vendas
          simuladas. As vendas apenas redistribuem o patrimônio entre classes.
        </p>
      ) : null}

      <div className="portfolio-comparison result-stage result-stage-comparison">
        <div>
          <span>Agora</span>
          <PortfolioStrip
            allocations={result.allocations}
            percentageKey="currentPercentage"
            label="Distribuição atual da carteira"
          />
        </div>
        <div>
          <span>
            {result.includeSales ? 'Após equalizar' : 'Após o aporte'}
          </span>
          <PortfolioStrip
            allocations={result.allocations}
            percentageKey="projectedPercentage"
            label="Distribuição projetada da carteira"
          />
        </div>
      </div>

      <div className="allocation-results result-stage result-stage-allocations">
        {result.allocations.map((allocation, index) => (
          <article
            className="allocation-result result-allocation-row"
            key={allocation.classId}
            style={{ animationDelay: `${260 + index * 55}ms` }}
          >
            <div className="allocation-result-title">
              <span
                className="color-key"
                style={{ backgroundColor: colorForIndex(index) }}
                aria-hidden="true"
              />
              <div>
                <h3>{allocation.name}</h3>
                <p>
                  {formatPercentage(allocation.currentPercentage)} agora · meta
                  de {formatPercentage(allocation.targetPercentage)}
                </p>
              </div>
              {result.includeSales ? (
                <div className="simulated-movement">
                  {allocation.suggestedPurchase !== '0.00' ? (
                    <>
                      <span>Compra simulada</span>
                      <strong>
                        {formatCurrency(allocation.suggestedPurchase)}
                      </strong>
                    </>
                  ) : allocation.suggestedSale !== '0.00' ? (
                    <>
                      <span>Venda simulada</span>
                      <strong className="simulated-sale">
                        {formatCurrency(allocation.suggestedSale)}
                      </strong>
                    </>
                  ) : (
                    <span>Sem movimentação</span>
                  )}
                </div>
              ) : (
                <strong>
                  {formatCurrency(allocation.suggestedContribution)}
                </strong>
              )}
            </div>
            <div className="progress-track" aria-hidden="true">
              <span
                className="progress-current result-progress-current"
                style={{
                  backgroundColor: colorForIndex(index),
                  width: barWidth(allocation.projectedPercentage),
                  animationDelay: `${300 + index * 55}ms`,
                }}
              />
              <i style={{ left: barWidth(allocation.targetPercentage) }} />
            </div>
            <p className="projected-copy">
              Projeção: {formatCurrency(allocation.projectedAmount)} ·{' '}
              {formatPercentage(allocation.projectedPercentage)}
            </p>
          </article>
        ))}
      </div>

      <p className="result-disclaimer result-stage result-stage-disclaimer">
        {result.disclaimer}
      </p>
    </div>
  )
}

export function ContributionSimulator({
  initialPreset,
}: {
  initialPreset?: SimulationDraftPreset
}) {
  const [allocations, setAllocations] = useState<DraftAllocation[]>(() =>
    (initialPreset?.allocations ?? INITIAL_ALLOCATIONS).map((allocation) => ({
      ...allocation,
    })),
  )
  const [contribution, setContribution] = useState(
    initialPreset?.contribution ?? '2000,00',
  )
  const [isDemoPreset, setIsDemoPreset] = useState(
    initialPreset?.source === 'demo',
  )
  const [includeSales, setIncludeSales] = useState(false)
  const [result, setResult] = useState<ContributionSimulationResponse | null>(
    null,
  )
  const [resultVersion, setResultVersion] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const nextClassNumber = useRef(
    Math.max(5, (initialPreset?.allocations.length ?? 4) + 1),
  )
  const activeRequest = useRef<AbortController | null>(null)
  const classNameInputs = useRef(new Map<string, HTMLInputElement>())
  const pendingFocusClassId = useRef<string | null>(null)

  useEffect(
    () => () => {
      activeRequest.current?.abort()
      activeRequest.current = null
    },
    [],
  )

  useLayoutEffect(() => {
    const classId = pendingFocusClassId.current
    if (classId !== null) {
      classNameInputs.current.get(classId)?.focus()
      pendingFocusClassId.current = null
    }
  }, [allocations])

  const targetSummary = useMemo(() => {
    const units = allocations.map((allocation) =>
      parseTargetUnits(allocation.targetPercentage),
    )
    const hasInvalidTarget = units.some((value) => value === null)
    const total = units.reduce<bigint>((sum, value) => sum + (value ?? 0n), 0n)
    return {
      total,
      hasInvalidTarget,
      isValid: !hasInvalidTarget && total === 1000000n,
    }
  }, [allocations])

  function updateAllocation(
    index: number,
    field: 'name' | 'currentAmount' | 'targetPercentage',
    value: string,
  ) {
    invalidateSimulation()
    setAllocations((current) =>
      current.map((allocation, allocationIndex) =>
        allocationIndex === index
          ? { ...allocation, [field]: value }
          : allocation,
      ),
    )
  }

  function commitAllocationMoney(index: number, value: string) {
    const completedValue = completeMoneyValue(value)

    if (completedValue !== value) {
      updateAllocation(index, 'currentAmount', completedValue)
    }
  }

  function addAllocation() {
    invalidateSimulation()
    const number = nextClassNumber.current
    nextClassNumber.current += 1
    const classId = `custom-class-${number}`
    pendingFocusClassId.current = classId
    setAllocations((current) => [
      ...current,
      {
        classId,
        name: `Classe ${number}`,
        currentAmount: '0,00',
        targetPercentage: '0',
      },
    ])
  }

  function removeAllocation(index: number) {
    invalidateSimulation()
    pendingFocusClassId.current =
      allocations[index + 1]?.classId ?? allocations[index - 1]?.classId ?? null
    setAllocations((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    )
  }

  function restoreExample() {
    invalidateSimulation()
    setAllocations(INITIAL_ALLOCATIONS.map((allocation) => ({ ...allocation })))
    setContribution('2000,00')
    setIncludeSales(false)
    setIsDemoPreset(false)
  }

  function invalidateSimulation() {
    activeRequest.current?.abort()
    activeRequest.current = null
    setIsLoading(false)
    setResult(null)
    setError(null)
  }

  function updateContribution(value: string) {
    invalidateSimulation()
    setContribution(value)
  }

  function commitContribution(value: string) {
    const completedValue = completeMoneyValue(value)

    if (completedValue !== value) {
      updateContribution(completedValue)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    activeRequest.current?.abort()
    const requestController = new AbortController()
    activeRequest.current = requestController
    setError(null)
    setIsLoading(true)

    const request: ContributionSimulationRequest = {
      currency: 'BRL',
      contribution: normalizeDecimal(contribution),
      includeSales,
      allocations: allocations.map((allocation) => ({
        classId: allocation.classId,
        name: allocation.name.trim(),
        currentAmount: normalizeDecimal(allocation.currentAmount),
        targetPercentage: normalizeDecimal(allocation.targetPercentage),
      })),
    }

    try {
      const simulation = await simulateContribution(
        request,
        requestController.signal,
      )
      if (activeRequest.current === requestController) {
        setResult(simulation)
        setResultVersion((currentVersion) => currentVersion + 1)
      }
    } catch (caughtError) {
      if (
        activeRequest.current === requestController &&
        !(caughtError instanceof Error && caughtError.name === 'AbortError')
      ) {
        setResult(null)
        setError(
          caughtError instanceof Error
            ? caughtError.message
            : 'Não foi possível concluir a simulação.',
        )
      }
    } finally {
      if (activeRequest.current === requestController) {
        activeRequest.current = null
        setIsLoading(false)
      }
    }
  }

  return (
    <section
      className="simulator"
      id="simulador"
      aria-labelledby="simulator-title"
    >
      <form
        aria-busy={isLoading}
        className="simulator-form"
        onSubmit={handleSubmit}
      >
        <div className="panel-heading">
          <div>
            <p className="section-index">01 / Configure</p>
            <h2 id="simulator-title">Sua carteira hoje</h2>
          </div>
          <button
            className="text-button"
            type="button"
            onClick={restoreExample}
          >
            Restaurar exemplo
          </button>
        </div>

        {isDemoPreset ? (
          <p className="preset-notice" role="note">
            Cenário iniciado com valores sintéticos da visão geral, incluindo
            caixa hipotético. Não é uma carteira salva.
          </p>
        ) : null}

        <div className="allocation-table-heading" aria-hidden="true">
          <span>Classe</span>
          <span>Valor atual</span>
          <span>Meta</span>
          <span />
        </div>

        <div className="allocation-fields">
          {allocations.map((allocation, index) => (
            <fieldset className="allocation-row" key={allocation.classId}>
              <legend>Classe de ativo {index + 1}</legend>
              <label className="class-field">
                <span>Classe</span>
                <i
                  className="color-key"
                  style={{ backgroundColor: colorForIndex(index) }}
                  aria-hidden="true"
                />
                <input
                  aria-label={`Nome da classe ${index + 1}`}
                  ref={(input) => {
                    if (input) {
                      classNameInputs.current.set(allocation.classId, input)
                    } else {
                      classNameInputs.current.delete(allocation.classId)
                    }
                  }}
                  maxLength={60}
                  onChange={(event) =>
                    updateAllocation(index, 'name', event.target.value)
                  }
                  required
                  type="text"
                  value={allocation.name}
                />
              </label>
              <label className="money-field">
                <span>Valor atual</span>
                <span className="input-with-prefix">
                  <b aria-hidden="true">R$</b>
                  <input
                    aria-label={`Valor atual de ${allocation.name}`}
                    inputMode="decimal"
                    onBlur={(event) =>
                      commitAllocationMoney(index, event.currentTarget.value)
                    }
                    onChange={(event) =>
                      updateAllocation(
                        index,
                        'currentAmount',
                        event.target.value,
                      )
                    }
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault()
                        event.currentTarget.blur()
                      }
                    }}
                    required
                    type="text"
                    value={allocation.currentAmount}
                  />
                </span>
              </label>
              <label className="target-field">
                <span>Meta</span>
                <span className="input-with-suffix">
                  <input
                    aria-label={`Meta percentual de ${allocation.name}`}
                    inputMode="decimal"
                    onChange={(event) =>
                      updateAllocation(
                        index,
                        'targetPercentage',
                        event.target.value,
                      )
                    }
                    required
                    type="text"
                    value={allocation.targetPercentage}
                  />
                  <b aria-hidden="true">%</b>
                </span>
              </label>
              <button
                aria-label={`Remover ${allocation.name}`}
                className="remove-button"
                disabled={allocations.length === 1}
                onClick={() => removeAllocation(index)}
                title={`Remover ${allocation.name}`}
                type="button"
              >
                ×
              </button>
            </fieldset>
          ))}
        </div>

        <div className="form-tools">
          <button
            className="add-button"
            disabled={allocations.length >= 20}
            onClick={addAllocation}
            type="button"
          >
            <span aria-hidden="true">+</span> Adicionar classe
          </button>
          <p
            className={
              targetSummary.isValid
                ? 'target-total valid'
                : 'target-total invalid'
            }
          >
            Soma das metas:{' '}
            <strong>
              {targetSummary.hasInvalidTarget
                ? 'valor inválido'
                : formatTargetTotal(targetSummary.total)}
            </strong>
            <span className="target-total-description">
              {targetSummary.isValid
                ? 'Metas válidas: somam exatamente 100%.'
                : targetSummary.hasInvalidTarget
                  ? 'Meta inválida: use valores entre 0 e 100 com até quatro casas decimais.'
                  : 'Metas inválidas: a soma deve ser exatamente 100%.'}
            </span>
          </p>
        </div>

        <div className="contribution-field">
          <label htmlFor="contribution">Quanto você quer aportar?</label>
          <p>
            {includeSales
              ? 'Dinheiro novo para somar à carteira. Use R$ 0,00 para simular apenas a redistribuição entre classes.'
              : 'Na simulação, o valor é distribuído somente entre as classes abaixo da meta projetada.'}
          </p>
          <span className="contribution-input">
            <b aria-hidden="true">R$</b>
            <input
              id="contribution"
              inputMode="decimal"
              onBlur={(event) => commitContribution(event.currentTarget.value)}
              onChange={(event) => updateContribution(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  event.currentTarget.blur()
                }
              }}
              required
              type="text"
              value={contribution}
            />
          </span>
        </div>

        <div className="sales-option">
          <label>
            <input
              aria-describedby="sales-mode-description"
              checked={includeSales}
              onChange={(event) => {
                invalidateSimulation()
                setIncludeSales(event.target.checked)
              }}
              type="checkbox"
            />
            <span>Incluir vendas para equalizar classes</span>
          </label>
          <p id="sales-mode-description">
            {includeSales
              ? 'Simulação educacional por classe, sem executar ordens. Não considera impostos, taxas, liquidez ou quantidades de ativos.'
              : 'Somente novos aportes. Nenhuma venda será simulada.'}
          </p>
        </div>

        {error ? (
          <p className="error-message" role="alert">
            {error}
          </p>
        ) : null}

        <button
          aria-busy={isLoading}
          className="submit-button"
          disabled={isLoading}
          type="submit"
        >
          <span>{isLoading ? 'Calculando…' : 'Simular distribuição'}</span>
          {isLoading ? (
            <span aria-hidden="true" className="submit-spinner" />
          ) : (
            <span aria-hidden="true">→</span>
          )}
        </button>
        {isLoading ? (
          <>
            <p className="loading-status">
              O serviço gratuito pode levar até dois minutos para iniciar.
            </p>
            <button
              className="cancel-button"
              type="button"
              onClick={invalidateSimulation}
            >
              Cancelar simulação
            </button>
          </>
        ) : null}
      </form>

      <p className="simulation-announcement" role="status" aria-atomic="true">
        {isLoading
          ? 'Calculando a simulação. Aguarde.'
          : result
            ? `Simulação concluída. Total projetado: ${formatCurrency(result.projectedTotal)}.`
            : ''}
      </p>

      <aside
        aria-busy={isLoading}
        className="simulator-result"
        aria-label="Resultado da simulação"
      >
        {result ? (
          <SimulationResult key={resultVersion} result={result} />
        ) : (
          <EmptyResult includeSales={includeSales} />
        )}
      </aside>
    </section>
  )
}
