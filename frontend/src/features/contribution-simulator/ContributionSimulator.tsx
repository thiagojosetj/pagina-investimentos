import { useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { simulateContribution } from './api'
import type {
  AllocationResult,
  ContributionSimulationRequest,
  ContributionSimulationResponse,
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

function parseForDisplay(value: string): number {
  const parsed = Number(normalizeDecimal(value))
  return Number.isFinite(parsed) ? parsed : 0
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
    <div className="portfolio-strip" role="img" aria-label={label}>
      {allocations.map((allocation, index) => (
        <span
          key={allocation.classId}
          style={{
            backgroundColor: colorForIndex(index),
            width: barWidth(allocation[percentageKey]),
          }}
          title={`${allocation.name}: ${formatPercentage(allocation[percentageKey])}`}
        />
      ))}
    </div>
  )
}

function EmptyResult() {
  return (
    <div className="empty-result">
      <span className="empty-result-number">01</span>
      <div>
        <p className="eyebrow">Como o cálculo funciona</p>
        <h2>O aporte segue os déficits da carteira.</h2>
        <p>
          O cálculo compara cada valor atual com a meta monetária projetada após
          o aporte. Classes acima da meta recebem zero; o restante é dividido
          proporcionalmente entre os déficits.
        </p>
        <ul>
          <li>Sem recomendação de venda</li>
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
    <div className="result-content" aria-live="polite">
      <div className="result-heading">
        <div>
          <p className="eyebrow">Plano calculado</p>
          <h2>Distribuição do novo aporte</h2>
        </div>
        <span className="method-label">Déficit proporcional</span>
      </div>

      <dl className="totals-grid">
        <div>
          <dt>Patrimônio atual</dt>
          <dd>{formatCurrency(result.currentTotal)}</dd>
        </div>
        <div className="highlight-total">
          <dt>Novo aporte</dt>
          <dd>+ {formatCurrency(result.contribution)}</dd>
        </div>
        <div>
          <dt>Total projetado</dt>
          <dd>{formatCurrency(result.projectedTotal)}</dd>
        </div>
      </dl>

      <div className="portfolio-comparison">
        <div>
          <span>Agora</span>
          <PortfolioStrip
            allocations={result.allocations}
            percentageKey="currentPercentage"
            label="Distribuição atual da carteira"
          />
        </div>
        <div>
          <span>Após o aporte</span>
          <PortfolioStrip
            allocations={result.allocations}
            percentageKey="projectedPercentage"
            label="Distribuição projetada da carteira"
          />
        </div>
      </div>

      <div className="allocation-results">
        {result.allocations.map((allocation, index) => (
          <article className="allocation-result" key={allocation.classId}>
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
              <strong>
                {formatCurrency(allocation.suggestedContribution)}
              </strong>
            </div>
            <div className="progress-track" aria-hidden="true">
              <span
                className="progress-current"
                style={{
                  backgroundColor: colorForIndex(index),
                  width: barWidth(allocation.projectedPercentage),
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

      <p className="result-disclaimer">{result.disclaimer}</p>
    </div>
  )
}

export function ContributionSimulator() {
  const [allocations, setAllocations] = useState<DraftAllocation[]>(() =>
    INITIAL_ALLOCATIONS.map((allocation) => ({ ...allocation })),
  )
  const [contribution, setContribution] = useState('2000,00')
  const [result, setResult] = useState<ContributionSimulationResponse | null>(
    null,
  )
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const nextClassNumber = useRef(5)

  const targetTotal = useMemo(
    () =>
      allocations.reduce(
        (total, allocation) =>
          total + parseForDisplay(allocation.targetPercentage),
        0,
      ),
    [allocations],
  )
  const isTargetTotalValid = Math.abs(targetTotal - 100) < 0.00005

  function updateAllocation(
    index: number,
    field: 'name' | 'currentAmount' | 'targetPercentage',
    value: string,
  ) {
    setAllocations((current) =>
      current.map((allocation, allocationIndex) =>
        allocationIndex === index
          ? { ...allocation, [field]: value }
          : allocation,
      ),
    )
    setResult(null)
    setError(null)
  }

  function addAllocation() {
    const number = nextClassNumber.current
    nextClassNumber.current += 1
    setAllocations((current) => [
      ...current,
      {
        classId: `custom-class-${number}`,
        name: `Classe ${number}`,
        currentAmount: '0,00',
        targetPercentage: '0',
      },
    ])
    setResult(null)
  }

  function removeAllocation(index: number) {
    setAllocations((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    )
    setResult(null)
    setError(null)
  }

  function restoreExample() {
    setAllocations(INITIAL_ALLOCATIONS.map((allocation) => ({ ...allocation })))
    setContribution('2000,00')
    setResult(null)
    setError(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsLoading(true)

    const request: ContributionSimulationRequest = {
      currency: 'BRL',
      contribution: normalizeDecimal(contribution),
      allocations: allocations.map((allocation) => ({
        classId: allocation.classId,
        name: allocation.name.trim(),
        currentAmount: normalizeDecimal(allocation.currentAmount),
        targetPercentage: normalizeDecimal(allocation.targetPercentage),
      })),
    }

    try {
      setResult(await simulateContribution(request))
    } catch (caughtError) {
      setResult(null)
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : 'Não foi possível concluir a simulação.',
      )
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <section
      className="simulator"
      id="simulador"
      aria-labelledby="simulator-title"
    >
      <form className="simulator-form" onSubmit={handleSubmit}>
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
                    onChange={(event) =>
                      updateAllocation(
                        index,
                        'currentAmount',
                        event.target.value,
                      )
                    }
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
              isTargetTotalValid ? 'target-total valid' : 'target-total invalid'
            }
          >
            Soma das metas:{' '}
            <strong>{formatPercentage(targetTotal.toFixed(4))}</strong>
          </p>
        </div>

        <div className="contribution-field">
          <label htmlFor="contribution">Quanto você quer aportar?</label>
          <p>
            O valor será direcionado somente às classes abaixo da meta
            projetada.
          </p>
          <span className="contribution-input">
            <b aria-hidden="true">R$</b>
            <input
              id="contribution"
              inputMode="decimal"
              onChange={(event) => {
                setContribution(event.target.value)
                setResult(null)
                setError(null)
              }}
              required
              type="text"
              value={contribution}
            />
          </span>
        </div>

        {error ? (
          <p className="error-message" role="alert">
            {error}
          </p>
        ) : null}

        <button className="submit-button" disabled={isLoading} type="submit">
          <span>{isLoading ? 'Calculando…' : 'Simular distribuição'}</span>
          <span aria-hidden="true">→</span>
        </button>
      </form>

      <aside className="simulator-result" aria-label="Resultado da simulação">
        {result ? <SimulationResult result={result} /> : <EmptyResult />}
      </aside>
    </section>
  )
}
