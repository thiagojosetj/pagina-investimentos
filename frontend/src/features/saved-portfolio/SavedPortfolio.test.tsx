import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SavedPortfolio } from './SavedPortfolio'
import type {
  SavedPortfolio as Portfolio,
  SaveTargetsRequest,
} from './contracts'

function examplePortfolio(): Portfolio {
  return {
    id: 'synthetic-portfolio-1',
    name: 'Carteira fictícia',
    currency: 'BRL',
    version: 0,
    createdAt: '2026-01-01T10:00:00Z',
    updatedAt: '2026-01-01T10:00:00Z',
    targets: [
      {
        id: 'stocks',
        name: 'Ações',
        targetPercentage: '40.0000',
        displayOrder: 0,
      },
      {
        id: 'fiis',
        name: 'FIIs',
        targetPercentage: '25.0000',
        displayOrder: 1,
      },
      {
        id: 'etfs',
        name: 'ETFs',
        targetPercentage: '15.0000',
        displayOrder: 2,
      },
      {
        id: 'fixed-income',
        name: 'Renda fixa',
        targetPercentage: '20.0000',
        displayOrder: 3,
      },
    ],
  }
}

function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function server(initial: Portfolio[] = [], authenticated = false) {
  const state = { portfolios: initial, authenticated, saveStatus: 200 }
  const fetchMock = vi.fn(async (path: string, options: RequestInit = {}) => {
    if (path === '/api/v1/session')
      return json({
        authenticated: state.authenticated,
        displayName: state.authenticated ? 'Usuário fictício' : undefined,
        loginMode: 'local',
        walletAvailable: true,
      })
    if (path === '/api/v1/csrf')
      return json({ headerName: 'X-CSRF-TOKEN', token: 'synthetic-csrf' })
    if (path === '/api/v1/local-session') {
      state.authenticated = true
      return new Response(null, { status: 204 })
    }
    if (path === '/api/v1/logout') {
      state.authenticated = false
      return new Response(null, { status: 204 })
    }
    if (path === '/api/v1/portfolios' && options.method === 'POST') {
      const body = JSON.parse(options.body as string) as { name: string }
      const portfolio = { ...examplePortfolio(), name: body.name }
      state.portfolios = [...state.portfolios, portfolio]
      return json(portfolio, 201)
    }
    if (path === '/api/v1/portfolios')
      return json({ portfolios: state.portfolios })
    if (path.endsWith('/targets')) {
      if (state.saveStatus !== 200) return json({}, state.saveStatus)
      const body = JSON.parse(options.body as string) as SaveTargetsRequest
      const existing = state.portfolios[0]!
      const portfolio = {
        ...existing,
        version: existing.version + 1,
        targets: body.targets.map((target, index) => ({
          ...target,
          id: target.id ?? `class-${index}`,
        })),
      }
      state.portfolios = [portfolio]
      return json(portfolio)
    }
    throw new Error(`Unexpected request: ${path}`)
  })
  vi.stubGlobal('fetch', fetchMock)
  return { state, fetchMock }
}

async function changeTargets(user: ReturnType<typeof userEvent.setup>) {
  const first = await screen.findByLabelText('Meta da classe 1')
  await user.clear(first)
  await user.type(first, '39')
  const second = screen.getByLabelText('Meta da classe 2')
  await user.clear(second)
  await user.type(second, '26')
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('SavedPortfolio', () => {
  it('focuses the new class name and associates validation with the save control', async () => {
    const user = userEvent.setup()
    server([examplePortfolio()], true)
    render(<SavedPortfolio />)
    const add = await screen.findByRole('button', { name: 'Adicionar classe' })
    add.focus()
    await user.keyboard('{Enter}')

    const name = screen.getByLabelText('Nome da classe 5')
    expect(name).toHaveFocus()
    const feedback = screen.getByRole('status', { name: 'Validação das metas' })
    expect(feedback).toHaveTextContent('Cada classe precisa de um nome')
    const save = screen.getByRole('button', { name: 'Salvar metas' })
    expect(save).toBeDisabled()
    expect(save).toHaveAccessibleDescription(/Cada classe precisa de um nome/)

    await user.type(name, 'Caixa fictício')
    expect(feedback).toHaveTextContent('Metas somam 100%')
    expect(save).toBeEnabled()
  })

  it('focuses the next class name after removal, or the previous name at the end', async () => {
    const user = userEvent.setup()
    server([examplePortfolio()], true)
    render(<SavedPortfolio />)
    const removeSecond = await screen.findByRole('button', {
      name: 'Remover classe 2',
    })
    removeSecond.focus()
    await user.keyboard('{Enter}')

    expect(screen.getByLabelText('Nome da classe 2')).toHaveValue('ETFs')
    expect(screen.getByLabelText('Nome da classe 2')).toHaveFocus()

    const removeLast = screen.getByRole('button', { name: 'Remover classe 3' })
    removeLast.focus()
    await user.keyboard(' ')
    expect(screen.getByLabelText('Nome da classe 2')).toHaveFocus()
    expect(screen.queryByLabelText('Nome da classe 3')).not.toBeInTheDocument()
  })

  it('keeps removal disabled when only one class remains', async () => {
    server(
      [
        {
          ...examplePortfolio(),
          targets: [
            {
              id: 'stocks',
              name: 'Ações',
              targetPercentage: '100.0000',
              displayOrder: 0,
            },
          ],
        },
      ],
      true,
    )
    render(<SavedPortfolio />)

    expect(
      await screen.findByRole('button', { name: 'Remover classe 1' }),
    ).toBeDisabled()
  })

  it('does not expose the previous wallet after a session refresh whose ownership list fails', async () => {
    const user = userEvent.setup()
    const { fetchMock, state } = server([examplePortfolio()], true)
    const original = fetchMock.getMockImplementation()!
    let failList = false
    fetchMock.mockImplementation(async (path, options = {}) => {
      if (path === '/api/v1/session' && failList)
        return json({
          authenticated: true,
          displayName: 'Outra sessão fictícia',
          loginMode: 'local',
          walletAvailable: true,
        })
      if (path === '/api/v1/portfolios' && failList) return json({}, 403)
      return original(path, options)
    })
    render(<SavedPortfolio />)
    await changeTargets(user)
    state.saveStatus = 403
    await user.click(screen.getByRole('button', { name: 'Salvar metas' }))
    await screen.findByRole('alert')
    failList = true
    await user.click(
      screen.getByRole('button', {
        name: 'Verificar sessão e tentar novamente',
      }),
    )
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Entrar com usuário fictício' }),
      ).toBeInTheDocument(),
    )
    expect(screen.queryByLabelText('Meta da classe 1')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Metas de Carteira fictícia' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByText('Outra sessão fictícia')).not.toBeInTheDocument()
  })

  it('requires an explicit discard before switching or creating a portfolio with unsaved targets', async () => {
    const user = userEvent.setup()
    const { fetchMock } = server(
      [
        examplePortfolio(),
        {
          ...examplePortfolio(),
          id: 'synthetic-portfolio-2',
          name: 'Segundo cenário',
        },
      ],
      true,
    )
    const confirm = vi.fn().mockReturnValue(false)
    vi.stubGlobal('confirm', confirm)
    render(<SavedPortfolio />)
    await changeTargets(user)
    await user.click(screen.getByRole('button', { name: /Segundo cenário/ }))
    expect(confirm).toHaveBeenCalledOnce()
    expect(screen.getByLabelText('Meta da classe 1')).toHaveValue('39')
    await user.type(
      screen.getByLabelText('Nome da nova carteira'),
      'Terceiro cenário',
    )
    await user.click(screen.getByRole('button', { name: 'Criar carteira' }))
    expect(confirm).toHaveBeenCalledTimes(2)
    expect(
      fetchMock.mock.calls.some(([, options]) => options?.method === 'POST'),
    ).toBe(false)
    confirm.mockReturnValue(true)
    await user.click(screen.getByRole('button', { name: /Segundo cenário/ }))
    expect(
      screen.getByRole('heading', { name: 'Metas de Segundo cenário' }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Meta da classe 1')).toHaveValue('40,0000')
  })

  it('returns to login when the initial portfolio load has an expired session', async () => {
    const { fetchMock } = server([examplePortfolio()], true)
    const original = fetchMock.getMockImplementation()!
    fetchMock.mockImplementation(async (path, options = {}) =>
      path === '/api/v1/portfolios' ? json({}, 401) : original(path, options),
    )
    render(<SavedPortfolio />)
    expect(
      await screen.findByRole('button', {
        name: 'Entrar com usuário fictício',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('sessão expirou')
    expect(
      screen.queryByRole('button', { name: 'Criar carteira' }),
    ).not.toBeInTheDocument()
  })

  it('keeps the public demo honest when saved wallets are disabled', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      json({
        authenticated: false,
        loginMode: 'demo',
        walletAvailable: false,
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    render(<SavedPortfolio />)

    expect(
      await screen.findByRole('heading', {
        name: 'Explore a demonstração sem criar uma conta.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Criar carteira' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'Entrar com Google' }),
    ).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('uses the configured Google authorization endpoint without storing tokens', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        json({
          authenticated: false,
          loginMode: 'google',
          walletAvailable: true,
        }),
      ),
    )
    render(<SavedPortfolio />)

    expect(
      await screen.findByRole('link', { name: 'Entrar com Google' }),
    ).toHaveAttribute('href', '/oauth2/authorization/google')
    expect(
      screen.queryByRole('button', { name: 'Entrar com usuário fictício' }),
    ).not.toBeInTheDocument()
  })

  it('logs into the synthetic session, creates, saves and reloads persistent targets', async () => {
    const user = userEvent.setup()
    const { state, fetchMock } = server()
    const rendered = render(<SavedPortfolio />)

    await user.click(
      await screen.findByRole('button', {
        name: 'Entrar com usuário fictício',
      }),
    )
    await user.type(
      await screen.findByLabelText('Nome da nova carteira'),
      'Meu cenário fictício',
    )
    await user.click(screen.getByRole('button', { name: 'Criar carteira' }))
    await changeTargets(user)
    await user.click(screen.getByRole('button', { name: 'Salvar metas' }))
    expect(
      await screen.findByText(/Metas salvas\. Elas estarão disponíveis/),
    ).toBeInTheDocument()
    expect(state.portfolios[0]?.targets[0]?.targetPercentage).toBe('39.0000')
    const requests = fetchMock.mock.calls.filter(
      ([, options]) => options?.method === 'POST' || options?.method === 'PUT',
    )
    expect(requests).toHaveLength(3)
    for (const [, options] of requests) {
      expect(options?.headers).toMatchObject({
        'X-CSRF-TOKEN': 'synthetic-csrf',
      })
      expect(options?.credentials).toBe('same-origin')
    }
    expect(
      fetchMock.mock.calls.filter(([path]) => path === '/api/v1/csrf'),
    ).toHaveLength(3)
    await user.click(
      screen.getByRole('button', { name: 'Recarregar metas salvas' }),
    )
    expect(
      await screen.findByText(
        'Carteira recarregada com as metas salvas no servidor.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Meta da classe 1')).toHaveValue('39,0000')

    rendered.unmount()
    render(<SavedPortfolio />)
    expect(await screen.findByLabelText('Meta da classe 1')).toHaveValue(
      '39,0000',
    )
    expect(
      screen.getByRole('heading', { name: 'Metas de Meu cenário fictício' }),
    ).toBeInTheDocument()
  }, 10000)

  it('preserves the draft after a conflict and reloads only on an explicit action', async () => {
    const user = userEvent.setup()
    const { state } = server([examplePortfolio()], true)
    state.saveStatus = 409
    render(<SavedPortfolio />)
    await changeTargets(user)
    await user.click(screen.getByRole('button', { name: 'Salvar metas' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('outra sessão')
    expect(screen.getByLabelText('Meta da classe 1')).toHaveValue('39')
    expect(screen.getByRole('button', { name: 'Salvar metas' })).toBeDisabled()
    await user.click(
      screen.getByRole('button', { name: 'Descartar rascunho e recarregar' }),
    )
    expect(
      await screen.findByText(
        'Carteira recarregada com as metas salvas no servidor.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Meta da classe 1')).toHaveValue('40,0000')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('handles session expiration and clears private data before a new login', async () => {
    const user = userEvent.setup()
    const { state } = server([examplePortfolio()], true)
    state.saveStatus = 401
    render(<SavedPortfolio />)
    await changeTargets(user)
    await user.click(screen.getByRole('button', { name: 'Salvar metas' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('sessão expirou')
    expect(screen.queryByLabelText('Meta da classe 1')).not.toBeInTheDocument()
    await user.click(
      screen.getByRole('button', { name: 'Entrar com usuário fictício' }),
    )
    expect(await screen.findByLabelText('Meta da classe 1')).toHaveValue(
      '40,0000',
    )
    expect(
      screen.getByText('Metas sincronizadas com a carteira salva.'),
    ).toBeInTheDocument()
  })

  it('shows an authorization failure without accepting a save or exposing backend details', async () => {
    const user = userEvent.setup()
    const { state } = server([examplePortfolio()], true)
    state.saveStatus = 403
    render(<SavedPortfolio />)
    await changeTargets(user)
    await user.click(screen.getByRole('button', { name: 'Salvar metas' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Não foi possível autorizar',
    )
    expect(screen.getByLabelText('Meta da classe 1')).toHaveValue('39')
    expect(state.portfolios[0]?.targets[0]?.targetPercentage).toBe('40.0000')
    expect(
      screen.queryByText(/Metas salvas\. Elas estarão disponíveis/),
    ).not.toBeInTheDocument()
  })

  it('aborts an in-flight save on logout and rejects its late response', async () => {
    const user = userEvent.setup()
    const { fetchMock } = server([examplePortfolio()], true)
    let resolveSave: ((response: Response) => void) | undefined
    let saveSignal: AbortSignal | null | undefined
    const original = fetchMock.getMockImplementation()!
    fetchMock.mockImplementation(async (path, options = {}) => {
      if (path.endsWith('/targets')) {
        saveSignal = options.signal
        return new Promise<Response>((resolve) => {
          resolveSave = resolve
        })
      }
      return original(path, options)
    })
    render(<SavedPortfolio />)
    await changeTargets(user)
    await user.click(screen.getByRole('button', { name: 'Salvar metas' }))
    await waitFor(() => expect(resolveSave).toBeDefined())
    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(saveSignal?.aborted).toBe(true)
    await act(async () => {
      resolveSave?.(json(examplePortfolio()))
    })
    expect(await screen.findByText(/Sessão encerrada/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Meta da classe 1')).not.toBeInTheDocument()
    expect(
      screen.queryByText(/Metas salvas\. Elas estarão disponíveis/),
    ).not.toBeInTheDocument()
  })

  it('offers a logout retry instead of claiming a failed logout succeeded', async () => {
    const user = userEvent.setup()
    const { fetchMock } = server([examplePortfolio()], true)
    const original = fetchMock.getMockImplementation()!
    let failLogout = true
    fetchMock.mockImplementation(async (path, options = {}) => {
      if (path === '/api/v1/logout' && failLogout) return json({}, 503)
      return original(path, options)
    })
    render(<SavedPortfolio />)
    await screen.findByLabelText('Meta da classe 1')
    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(
      await screen.findByRole('button', {
        name: 'Tentar encerrar sessão novamente',
      }),
    ).toBeInTheDocument()
    expect(screen.queryByText(/Sessão encerrada/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Meta da classe 1')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Entrar com usuário fictício' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', {
        name: 'Verificar sessão e tentar novamente',
      }),
    ).not.toBeInTheDocument()
    failLogout = false
    await user.click(
      screen.getByRole('button', { name: 'Tentar encerrar sessão novamente' }),
    )
    expect(await screen.findByText(/Sessão encerrada/)).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Entrar com usuário fictício' }),
    ).toBeInTheDocument()
  })
})
