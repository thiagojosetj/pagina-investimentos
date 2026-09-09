import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  window.localStorage.clear()
  document.documentElement.removeAttribute('data-theme')
  document.documentElement.style.removeProperty('color-scheme')
})

describe('App', () => {
  it('opens with the portfolio overview and navigates to the simulator', async () => {
    const user = userEvent.setup()
    render(<App />)

    expect(
      screen.getByRole('heading', {
        name: 'Sua carteira, organizada em um só lugar.',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Dados sintéticos')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Simulador' }))

    expect(screen.getByText('Módulo inicial')).toBeInTheDocument()
    expect(
      screen.getByText(/primeiro módulo de uma plataforma de carteira/i),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/carteiras salvas, ativos e movimentações ainda não/i),
    ).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('main')).toHaveFocus())
  })

  it('preserves the simulator draft and result when switching views', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            method: 'PROPORTIONAL_MONETARY_DEFICIT_V1',
            currency: 'BRL',
            currentTotal: '10000.00',
            contribution: '2500.00',
            projectedTotal: '12500.00',
            allocations: [
              {
                classId: 'stocks',
                name: 'Ações',
                currentAmount: '4800.00',
                currentPercentage: '48.0000',
                targetPercentage: '40.0000',
                targetAmount: '5000.00',
                monetaryDeficit: '200.00',
                suggestedContribution: '200.00',
                projectedAmount: '5000.00',
                projectedPercentage: '40.0000',
              },
              {
                classId: 'real-estate-funds',
                name: 'FIIs',
                currentAmount: '1800.00',
                currentPercentage: '18.0000',
                targetPercentage: '25.0000',
                targetAmount: '3125.00',
                monetaryDeficit: '1325.00',
                suggestedContribution: '1325.00',
                projectedAmount: '3125.00',
                projectedPercentage: '25.0000',
              },
              {
                classId: 'etfs',
                name: 'ETFs',
                currentAmount: '1400.00',
                currentPercentage: '14.0000',
                targetPercentage: '15.0000',
                targetAmount: '1875.00',
                monetaryDeficit: '475.00',
                suggestedContribution: '475.00',
                projectedAmount: '1875.00',
                projectedPercentage: '15.0000',
              },
              {
                classId: 'fixed-income',
                name: 'Renda fixa',
                currentAmount: '2000.00',
                currentPercentage: '20.0000',
                targetPercentage: '20.0000',
                targetAmount: '2500.00',
                monetaryDeficit: '500.00',
                suggestedContribution: '500.00',
                projectedAmount: '2500.00',
                projectedPercentage: '20.0000',
              },
            ],
            disclaimer: 'Simulação educacional.',
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Simulador' }))
    const contribution = screen.getByLabelText('Quanto você quer aportar?')
    await user.clear(contribution)
    await user.type(contribution, '2500,00')
    await user.click(
      screen.getByRole('button', { name: 'Simular distribuição' }),
    )
    expect(
      await screen.findByRole('heading', {
        name: 'Distribuição do novo aporte',
      }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Visão geral' }))
    await user.click(screen.getByRole('button', { name: 'Simulador' }))

    expect(screen.getByLabelText('Quanto você quer aportar?')).toHaveValue(
      '2500,00',
    )
    expect(
      screen.getByRole('heading', { name: 'Distribuição do novo aporte' }),
    ).toBeInTheDocument()
  })

  it('avoids smooth scrolling when reduced motion is preferred', async () => {
    const user = userEvent.setup()
    const scrollIntoView = vi.fn()
    const previousScrollIntoView = Object.getOwnPropertyDescriptor(
      HTMLElement.prototype,
      'scrollIntoView',
    )

    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: true })),
    )
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    })

    try {
      render(<App />)
      await user.click(screen.getByRole('button', { name: 'Simulador' }))

      await waitFor(() =>
        expect(scrollIntoView).toHaveBeenCalledWith({ behavior: 'auto' }),
      )
    } finally {
      if (previousScrollIntoView) {
        Object.defineProperty(
          HTMLElement.prototype,
          'scrollIntoView',
          previousScrollIntoView,
        )
      } else {
        Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView')
      }
    }
  })

  it('switches color theme and persists the visual preference', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: false })),
    )

    render(<App />)

    expect(document.documentElement).toHaveAttribute('data-theme', 'light')
    await user.click(screen.getByRole('button', { name: 'Ativar modo escuro' }))

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
    expect(window.localStorage.getItem('portfolio-planner-theme')).toBe('dark')
    expect(
      screen.getByRole('button', { name: 'Ativar modo claro' }),
    ).toBeInTheDocument()
  })

  it('restores a previously saved color theme', () => {
    window.localStorage.setItem('portfolio-planner-theme', 'dark')
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: false })),
    )

    render(<App />)

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
    expect(
      screen.getByRole('button', { name: 'Ativar modo claro' }),
    ).toBeInTheDocument()
  })

  it('uses the system color theme when no preference was saved', () => {
    const matchMedia = vi.fn((query: string) => ({
      matches: query === '(prefers-color-scheme: dark)',
    }))
    vi.stubGlobal('matchMedia', matchMedia)

    render(<App />)

    expect(matchMedia).toHaveBeenCalledWith('(prefers-color-scheme: dark)')
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
  })
})
