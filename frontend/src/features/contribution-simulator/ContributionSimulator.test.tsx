import { act, cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ContributionSimulator } from './ContributionSimulator'
import type { ContributionSimulationResponse } from './contracts'
import { createDemoSimulationPreset } from '../portfolio-overview/demoPortfolio'

const successfulResponse: ContributionSimulationResponse = {
  method: 'PROPORTIONAL_MONETARY_DEFICIT_V1',
  currency: 'BRL',
  currentTotal: '10000.00',
  contribution: '2000.00',
  projectedTotal: '12000.00',
  includeSales: false,
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
      suggestedPurchase: '0.00',
      suggestedSale: '0.00',
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
      suggestedPurchase: '1200.00',
      suggestedSale: '0.00',
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
      suggestedPurchase: '400.00',
      suggestedSale: '0.00',
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
      suggestedPurchase: '400.00',
      suggestedSale: '0.00',
      projectedAmount: '2400.00',
      projectedPercentage: '20.0000',
    },
  ],
  disclaimer:
    'Simulação educacional baseada exclusivamente nas metas informadas.',
}

const salesResponse: ContributionSimulationResponse = {
  ...successfulResponse,
  method: 'TARGET_CLASS_REBALANCING_WITH_SIMULATED_SALES_V1',
  contribution: '500.00',
  projectedTotal: '10500.00',
  includeSales: true,
  allocations: [
    {
      ...successfulResponse.allocations[0],
      targetAmount: '4200.00',
      suggestedContribution: '0.00',
      suggestedPurchase: '0.00',
      suggestedSale: '600.00',
      projectedAmount: '4200.00',
      projectedPercentage: '40.0000',
    },
    {
      ...successfulResponse.allocations[1],
      targetAmount: '2625.00',
      monetaryDeficit: '825.00',
      suggestedContribution: '375.00',
      suggestedPurchase: '825.00',
      suggestedSale: '0.00',
      projectedAmount: '2625.00',
      projectedPercentage: '25.0000',
    },
    {
      ...successfulResponse.allocations[2],
      targetAmount: '1575.00',
      monetaryDeficit: '175.00',
      suggestedContribution: '79.55',
      suggestedPurchase: '175.00',
      suggestedSale: '0.00',
      projectedAmount: '1575.00',
      projectedPercentage: '15.0000',
    },
    {
      ...successfulResponse.allocations[3],
      targetAmount: '2100.00',
      monetaryDeficit: '100.00',
      suggestedContribution: '45.45',
      suggestedPurchase: '100.00',
      suggestedSale: '0.00',
      projectedAmount: '2100.00',
      projectedPercentage: '20.0000',
    },
  ],
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('ContributionSimulator', () => {
  it('starts with the fictitious example and explains the calculation', () => {
    render(<ContributionSimulator />)

    expect(screen.getByLabelText('Valor atual de Ações')).toHaveValue('4800,00')
    expect(
      screen.getByRole('checkbox', {
        name: 'Incluir vendas para equalizar classes',
      }),
    ).not.toBeChecked()
    expect(screen.getByText(/Somente novos aportes/)).toBeInTheDocument()
    expect(screen.getByText(/Soma das metas:/)).toHaveTextContent('100,00%')
    expect(
      screen.getByText(
        'O aporte é simulado a partir dos déficits da carteira.',
      ),
    ).toBeInTheDocument()
  })

  it('restores the original example after starting with the synthetic overview', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(
      <ContributionSimulator initialPreset={createDemoSimulationPreset()} />,
    )

    expect(screen.getAllByRole('group')).toHaveLength(5)
    expect(screen.getByLabelText('Valor atual de Caixa')).toHaveValue('3000,00')
    expect(screen.getByRole('note')).toHaveTextContent('caixa hipotético')
    const salesOption = screen.getByRole('checkbox', {
      name: 'Incluir vendas para equalizar classes',
    })
    expect(salesOption).not.toBeChecked()
    await user.click(salesOption)

    await user.click(screen.getByRole('button', { name: 'Restaurar exemplo' }))

    expect(screen.getAllByRole('group')).toHaveLength(4)
    expect(screen.getByLabelText('Valor atual de Ações')).toHaveValue('4800,00')
    expect(screen.queryByRole('note')).not.toBeInTheDocument()
    expect(salesOption).not.toBeChecked()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('focuses the new class name after adding a class', async () => {
    const user = userEvent.setup()
    render(<ContributionSimulator />)

    await user.click(screen.getByRole('button', { name: 'Adicionar classe' }))

    expect(screen.getByLabelText('Nome da classe 5')).toHaveFocus()
    expect(screen.getByLabelText('Nome da classe 5')).toHaveValue('Classe 5')
  })

  it('focuses the following class name after removing a middle class', async () => {
    const user = userEvent.setup()
    render(<ContributionSimulator />)

    await user.click(screen.getByRole('button', { name: 'Remover FIIs' }))

    expect(screen.getByLabelText('Nome da classe 2')).toHaveFocus()
    expect(screen.getByLabelText('Nome da classe 2')).toHaveValue('ETFs')
  })

  it('focuses the previous class name after removing the last class', async () => {
    const user = userEvent.setup()
    render(<ContributionSimulator />)

    await user.click(screen.getByRole('button', { name: 'Remover Renda fixa' }))

    expect(screen.getByLabelText('Nome da classe 3')).toHaveFocus()
    expect(screen.getByLabelText('Nome da classe 3')).toHaveValue('ETFs')
  })

  it('keeps one class and does not remove it when its button is disabled', async () => {
    const user = userEvent.setup()
    render(<ContributionSimulator />)

    await user.click(screen.getByRole('button', { name: 'Remover Renda fixa' }))
    await user.click(screen.getByRole('button', { name: 'Remover ETFs' }))
    await user.click(screen.getByRole('button', { name: 'Remover FIIs' }))

    const removeLastClass = screen.getByRole('button', {
      name: 'Remover Ações',
    })
    expect(removeLastClass).toBeDisabled()
    expect(screen.getByLabelText('Nome da classe 1')).toHaveFocus()
    await user.click(removeLastClass)

    expect(screen.getAllByRole('group')).toHaveLength(1)
    expect(screen.getByLabelText('Nome da classe 1')).toHaveValue('Ações')
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
    const requestOptions = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect(JSON.parse(requestOptions.body as string)).toMatchObject({
      includeSales: false,
    })
  })

  it('includes simulated purchases and sales only when selected', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(salesResponse), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    render(<ContributionSimulator />)

    await user.click(
      screen.getByRole('checkbox', {
        name: 'Incluir vendas para equalizar classes',
      }),
    )
    const contribution = screen.getByLabelText('Quanto você quer aportar?')
    await user.clear(contribution)
    await user.type(contribution, '500')
    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )

    expect(
      await screen.findByText('Equalização por classe'),
    ).toBeInTheDocument()
    const requestOptions = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect(JSON.parse(requestOptions.body as string)).toMatchObject({
      includeSales: true,
      contribution: '500.00',
    })
    const stocks = screen
      .getByRole('heading', { name: 'Ações' })
      .closest('article')!
    expect(within(stocks).getByText('Venda simulada')).toBeInTheDocument()
    expect(within(stocks).getByText('R$ 600,00')).toBeInTheDocument()
    const funds = screen
      .getByRole('heading', { name: 'FIIs' })
      .closest('article')!
    expect(within(funds).getByText('Compra simulada')).toBeInTheDocument()
    expect(within(funds).getByText('R$ 825,00')).toBeInTheDocument()
    expect(screen.queryByText('R$ 375,00')).not.toBeInTheDocument()
    expect(screen.getByText('+ R$ 500,00')).toBeInTheDocument()
    expect(
      screen.getByText(/As vendas apenas redistribuem/),
    ).toBeInTheDocument()
  })

  it('clears a calculated result when changing the sales option', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(successfulResponse), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    render(<ContributionSimulator />)
    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )
    expect(
      await screen.findByText('Distribuição do novo aporte'),
    ).toBeInTheDocument()

    await user.click(
      screen.getByRole('checkbox', {
        name: 'Incluir vendas para equalizar classes',
      }),
    )

    expect(
      screen.queryByText('Distribuição do novo aporte'),
    ).not.toBeInTheDocument()
    expect(
      screen.getByText(
        'Compras e vendas simuladas aproximam as classes das suas metas.',
      ),
    ).toBeInTheDocument()
  })

  it('cancels and ignores a pending calculation when changing the sales option', async () => {
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

    await user.click(
      screen.getByRole('checkbox', {
        name: 'Incluir vendas para equalizar classes',
      }),
    )

    expect(requestOptions.signal?.aborted).toBe(true)
    expect(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    ).toBeEnabled()
    await act(async () => {
      resolveRequest(
        new Response(JSON.stringify(successfulResponse), { status: 200 }),
      )
      await pendingRequest
    })
    expect(
      screen.queryByText('Distribuição do novo aporte'),
    ).not.toBeInTheDocument()
  })

  it('accepts zero new cash and identifies classes without movement', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          ...successfulResponse,
          method: 'TARGET_CLASS_REBALANCING_WITH_SIMULATED_SALES_V1',
          includeSales: true,
          currentTotal: '12000.00',
          contribution: '0.00',
          allocations: successfulResponse.allocations.map((allocation) => ({
            ...allocation,
            currentAmount: allocation.projectedAmount,
            currentPercentage: allocation.projectedPercentage,
            suggestedPurchase: '0.00',
            suggestedSale: '0.00',
            suggestedContribution: '0.00',
          })),
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)
    render(
      <ContributionSimulator
        initialPreset={{
          source: 'demo',
          contribution: '0,00',
          allocations: successfulResponse.allocations.map((allocation) => ({
            classId: allocation.classId,
            name: allocation.name,
            currentAmount: allocation.projectedAmount.replace('.', ','),
            targetPercentage: allocation.targetPercentage.replace('.', ','),
          })),
        }}
      />,
    )
    const salesOption = screen.getByRole('checkbox', {
      name: 'Incluir vendas para equalizar classes',
    })
    await user.click(salesOption)
    expect(salesOption).toHaveAccessibleDescription(
      /Não considera impostos, taxas, liquidez ou quantidades/,
    )
    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )

    expect(
      await screen.findByText('Equalização por classe'),
    ).toBeInTheDocument()
    expect(screen.getAllByText('Sem movimentação')).toHaveLength(4)
    expect(screen.queryByText('Compra simulada')).not.toBeInTheDocument()
    expect(screen.queryByText('Venda simulada')).not.toBeInTheDocument()
    const requestOptions = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect(JSON.parse(requestOptions.body as string)).toMatchObject({
      includeSales: true,
      contribution: '0.00',
    })
  })

  it('completes cents when a monetary input is confirmed', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<ContributionSimulator />)

    const currentAmount = screen.getByLabelText('Valor atual de Ações')
    const targetPercentage = screen.getByLabelText('Meta percentual de Ações')

    expect(currentAmount.parentElement).toHaveClass('input-with-prefix')
    expect(targetPercentage.parentElement).toHaveClass('input-with-suffix')

    await user.clear(currentAmount)
    await user.type(currentAmount, '4800')
    await user.keyboard('{Enter}')

    expect(currentAmount).toHaveValue('4800,00')

    const contribution = screen.getByLabelText('Quanto você quer aportar?')
    await user.clear(contribution)
    await user.type(contribution, '2500')
    await user.tab()

    expect(contribution).toHaveValue('2500,00')
    expect(fetchMock).not.toHaveBeenCalled()
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
    expect(screen.getByRole('button', { name: 'Calculando…' })).toHaveAttribute(
      'aria-busy',
      'true',
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'Calculando a simulação. Aguarde.',
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

  it('explains temporary API unavailability without local setup instructions', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('Bad Gateway', {
          status: 502,
          headers: { 'Content-Type': 'text/plain' },
        }),
      ),
    )
    render(<ContributionSimulator />)

    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'A API está temporariamente indisponível. Aguarde um momento e tente novamente.',
    )
  })

  it('explains a network failure without local setup instructions', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new TypeError('Failed to fetch')),
    )
    render(<ContributionSimulator />)

    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Não foi possível conectar à API. Aguarde um momento e tente novamente.',
    )
  })
})
