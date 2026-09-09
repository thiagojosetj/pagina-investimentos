import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PortfolioOverview } from './PortfolioOverview'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('PortfolioOverview', () => {
  it('shows a clearly synthetic portfolio with fixed income and cash', () => {
    render(<PortfolioOverview onOpenSimulator={vi.fn()} />)

    expect(screen.getByText('Fixture sintética')).toBeInTheDocument()
    expect(screen.getByText('R$ 60.000,00')).toBeInTheDocument()
    expect(screen.getByText('Título público — exemplo')).toBeInTheDocument()
    expect(screen.getByText('CDB Banco Modelo — exemplo')).toBeInTheDocument()
    expect(screen.getByText('Caixa disponível — exemplo')).toBeInTheDocument()
    expect(
      screen.getByRole('table', { name: 'Posições demonstrativas' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/sem persistência, cotações, rentabilidade/i),
    ).toBeInTheDocument()
  })

  it('filters the visible items by category without requesting external data', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<PortfolioOverview onOpenSimulator={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Renda fixa' }))

    expect(screen.getByText('Título público — exemplo')).toBeInTheDocument()
    expect(screen.getByText('CDB Banco Modelo — exemplo')).toBeInTheDocument()
    expect(
      screen.queryByText('Empresa Horizonte — exemplo'),
    ).not.toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('opens the simulator from the allocation context', async () => {
    const user = userEvent.setup()
    const onOpenSimulator = vi.fn()
    render(<PortfolioOverview onOpenSimulator={onOpenSimulator} />)

    await user.click(screen.getByRole('button', { name: /Abrir simulador/ }))

    expect(onOpenSimulator).toHaveBeenCalledOnce()
  })
})
