import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AssetExplorer } from './AssetExplorer'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('AssetExplorer', () => {
  it('opens a synthetic asset page without selecting an asset or requesting data', () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<AssetExplorer />)

    expect(
      screen.getByRole('heading', {
        name: 'Explore os ativos da demonstração.',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Mostrando 7 de 7 ativos.',
    )
    expect(screen.getByText('R$ 57.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 60.000,00')).toBeInTheDocument()
    expect(screen.getByText('Referência fixa: 02/09/2026')).toBeInTheDocument()
    expect(
      screen.getAllByRole('button', { name: /Ver detalhes de/ }),
    ).toHaveLength(7)
    expect(screen.queryByText('SYN-CASH')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Caixa' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Fechar detalhes' }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByText('Um ativo, uma visão mais próxima.'),
    ).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('combines accent-insensitive search with category filtering and preserves input focus', async () => {
    const user = userEvent.setup()
    render(<AssetExplorer />)
    const search = screen.getByRole('searchbox', { name: 'Buscar ativos' })

    await user.click(screen.getByRole('button', { name: 'FIIs' }))
    await user.type(search, ' V E R T I C E ')

    expect(search).toHaveFocus()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Mostrando 1 de 7 ativos.',
    )
    expect(screen.getByText('SYN-FII-02')).toBeInTheDocument()
    expect(screen.queryByText('SYN-FII-01')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'FIIs' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('shows honest details and restores focus to the triggering button when closing', async () => {
    const user = userEvent.setup()
    render(<AssetExplorer />)
    const trigger = screen.getByRole('button', {
      name: 'Ver detalhes de SYN-FII-01',
    })

    await user.click(trigger)
    const details = screen.getByRole('region', {
      name: 'Detalhes do ativo',
    })
    expect(trigger).toHaveAttribute('aria-expanded', 'true')
    expect(
      within(details).getByRole('heading', { name: 'Detalhes do ativo' }),
    ).toHaveFocus()
    expect(
      within(details).getByText('Fundo Mosaico — exemplo'),
    ).toBeInTheDocument()
    expect(within(details).getByText('R$ 7.000,00')).toBeInTheDocument()
    expect(within(details).getByText('11,67%')).toBeInTheDocument()
    expect(within(details).getByText('02/09/2026')).toBeInTheDocument()
    expect(
      within(details).getByText(/não contém quantidade, preço unitário/),
    ).toBeInTheDocument()
    expect(
      within(details).getByText(/Participação predefinida sobre R\$ 60.000,00/),
    ).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await user.click(
      within(details).getByRole('button', { name: 'Fechar detalhes' }),
    )
    expect(trigger).toHaveFocus()
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
    expect(
      within(details).getByText('Um ativo, uma visão mais próxima.'),
    ).toBeInTheDocument()
  })

  it('clears an invisible selection permanently when a category hides it', async () => {
    const user = userEvent.setup()
    render(<AssetExplorer />)
    await user.click(
      screen.getByRole('button', { name: 'Ver detalhes de SYN-FII-01' }),
    )
    const stocks = screen.getByRole('button', { name: 'Ações' })
    await user.click(stocks)

    expect(stocks).toHaveFocus()
    expect(
      screen.queryByRole('button', { name: 'Fechar detalhes' }),
    ).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    expect(screen.getByRole('status')).toHaveTextContent(
      'Mostrando 7 de 7 ativos.',
    )
    expect(
      screen.getByText('Um ativo, uma visão mais próxima.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Ver detalhes de SYN-FII-01' }),
    ).toHaveAttribute('aria-expanded', 'false')
  })

  it('shows empty results and restores all fixture items without bringing back a hidden selection', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<AssetExplorer />)
    await user.click(
      screen.getByRole('button', { name: 'Ver detalhes de SYN-ETF-01' }),
    )
    const search = screen.getByRole('searchbox', { name: 'Buscar ativos' })
    await user.type(search, 'não existe')

    expect(search).toHaveFocus()
    expect(screen.getByText('Nenhum ativo encontrado')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Mostrando 0 de 7 ativos.',
    )
    expect(
      screen.queryByRole('button', { name: 'Fechar detalhes' }),
    ).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    expect(search).toHaveValue('')
    expect(search).toHaveFocus()
    expect(
      screen.getAllByRole('button', { name: /Ver detalhes de/ }),
    ).toHaveLength(7)
    expect(screen.getByRole('button', { name: 'Todos' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(
      screen.queryByRole('button', { name: 'Fechar detalhes' }),
    ).not.toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('keeps a visible selection while sorting and never recalculates percentages based on filtered results', async () => {
    const user = userEvent.setup()
    render(<AssetExplorer />)
    await user.click(
      screen.getByRole('button', { name: 'Ver detalhes de SYN-FII-01' }),
    )
    await user.click(screen.getByRole('button', { name: 'FIIs' }))
    await user.selectOptions(
      screen.getByRole('combobox', { name: 'Ordenar ativos' }),
      'name',
    )

    const details = screen.getByRole('region', {
      name: 'Detalhes do ativo',
    })
    expect(
      within(details).getByText('Fundo Mosaico — exemplo'),
    ).toBeInTheDocument()
    expect(within(details).getByText('11,67%')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Mostrando 2 de 7 ativos.',
    )
    expect(
      screen.getByRole('combobox', { name: 'Ordenar ativos' }),
    ).toHaveFocus()
  })

  it('supports activating details and closing them using the keyboard', async () => {
    const user = userEvent.setup()
    render(<AssetExplorer />)
    const trigger = screen.getByRole('button', {
      name: 'Ver detalhes de SYN-CDB-01',
    })
    trigger.focus()
    await user.keyboard('{Enter}')
    const close = screen.getByRole('button', { name: 'Fechar detalhes' })
    close.focus()
    await user.keyboard(' ')

    expect(trigger).toHaveFocus()
    expect(
      screen.queryByRole('button', { name: 'Fechar detalhes' }),
    ).not.toBeInTheDocument()
  })
})
