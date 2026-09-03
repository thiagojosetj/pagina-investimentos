import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ContributionSimulator } from './ContributionSimulator'
import type { ContributionSimulationResponse } from './contracts'

const successfulResponse: ContributionSimulationResponse = {
  method: 'PROPORTIONAL_MONETARY_DEFICIT_V1',
  currency: 'BRL',
  currentTotal: '10000.00',
  contribution: '2000.00',
  projectedTotal: '12000.00',
  allocations: [
    {
      classId: 'stocks',
      name: 'Ações',
      currentAmount: '4800.00',
      currentPercentage: '48.0000',
      targetPercentage: '40.0000',
      targetAmount: '4800.00',
      monetaryDeficit: '0.00',
      suggestedContribution: '0.00',
      projectedAmount: '4800.00',
      projectedPercentage: '40.0000',
    },
    {
      classId: 'real-estate-funds',
      name: 'FIIs',
      currentAmount: '1800.00',
      currentPercentage: '18.0000',
      targetPercentage: '25.0000',
      targetAmount: '3000.00',
      monetaryDeficit: '1200.00',
      suggestedContribution: '1200.00',
      projectedAmount: '3000.00',
      projectedPercentage: '25.0000',
    },
    {
      classId: 'etfs',
      name: 'ETFs',
      currentAmount: '1400.00',
      currentPercentage: '14.0000',
      targetPercentage: '15.0000',
      targetAmount: '1800.00',
      monetaryDeficit: '400.00',
      suggestedContribution: '400.00',
      projectedAmount: '1800.00',
      projectedPercentage: '15.0000',
    },
    {
      classId: 'fixed-income',
      name: 'Renda fixa',
      currentAmount: '2000.00',
      currentPercentage: '20.0000',
      targetPercentage: '20.0000',
      targetAmount: '2400.00',
      monetaryDeficit: '400.00',
      suggestedContribution: '400.00',
      projectedAmount: '2400.00',
      projectedPercentage: '20.0000',
    },
  ],
  disclaimer:
    'Simulação educacional baseada exclusivamente nas metas informadas.',
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('ContributionSimulator', () => {
  it('starts with the fictitious example and explains the calculation', () => {
    render(<ContributionSimulator />)

    expect(screen.getByLabelText('Valor atual de Ações')).toHaveValue('4800,00')
    expect(screen.getByText(/Soma das metas:/)).toHaveTextContent('100,00%')
    expect(
      screen.getByText(
        'O aporte é simulado a partir dos déficits da carteira.',
      ),
    ).toBeInTheDocument()
  })

  it('sends normalized decimal strings and presents the calculated plan', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(successfulResponse), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    render(<ContributionSimulator />)

    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )

    expect(
      await screen.findByText('Distribuição do novo aporte'),
    ).toBeInTheDocument()
    expect(screen.getByText('+ R$ 2.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 1.200,00')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/allocation-simulations/contributions',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"contribution":"2000.00"'),
      }),
    )
  })

  it('ignores a pending response after the user changes an input', async () => {
    const user = userEvent.setup()
    let resolveRequest!: (response: Response) => void
    const pendingRequest = new Promise<Response>((resolve) => {
      resolveRequest = resolve
    })
    const fetchMock = vi.fn().mockReturnValue(pendingRequest)
    vi.stubGlobal('fetch', fetchMock)
    render(<ContributionSimulator />)

    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )
    const requestOptions = fetchMock.mock.calls[0]?.[1] as RequestInit

    await user.clear(screen.getByLabelText('Quanto você quer aportar?'))

    expect(requestOptions.signal).toBeInstanceOf(AbortSignal)
    expect(requestOptions.signal?.aborted).toBe(true)

    await act(async () => {
      resolveRequest(
        new Response(JSON.stringify(successfulResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      await pendingRequest
      await new Promise((resolve) => setTimeout(resolve, 0))
    })

    expect(
      screen.queryByText('Distribuição do novo aporte'),
    ).not.toBeInTheDocument()
  })

  it('shows the API validation detail without presenting stale results', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: 'Simulação inválida',
            detail: 'A soma das metas deve ser exatamente 100.0000%.',
          }),
          {
            status: 422,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    )
    render(<ContributionSimulator />)

    const target = screen.getByLabelText('Meta percentual de Ações')
    await user.clear(target)
    await user.type(target, '39')
    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'A soma das metas deve ser exatamente 100.0000%.',
    )
    expect(
      screen.queryByText('Distribuição do novo aporte'),
    ).not.toBeInTheDocument()
  })
})
