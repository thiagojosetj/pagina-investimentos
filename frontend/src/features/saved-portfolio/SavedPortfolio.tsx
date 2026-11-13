import {
  useEffect,
  useEffectEvent,
  useLayoutEffect,
  useRef,
  useState,
} from 'react'
import {
  createPortfolio,
  endSession,
  readPortfolios,
  readSession,
  saveTargets,
  startLocalSession,
  WalletApiError,
} from './api'
import { createTargetDraft, targetsRequest, validateTargets } from './contracts'
import type {
  SavedPortfolio as Portfolio,
  TargetDraft,
  WalletSession,
} from './contracts'
import './SavedPortfolio.css'

export function SavedPortfolio({ active = true }: { active?: boolean }) {
  const [session, setSession] = useState<WalletSession | null>(null)
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [selected, setSelected] = useState<Portfolio | null>(null)
  const [draft, setDraft] = useState<TargetDraft[]>([])
  const [name, setName] = useState('')
  const [dirty, setDirty] = useState(false)
  const [pending, setPending] = useState(true)
  const [error, setError] = useState<WalletApiError | null>(null)
  const [notice, setNotice] = useState('')
  const [logoutFailed, setLogoutFailed] = useState(false)
  const [verifyingSession, setVerifyingSession] = useState(false)
  const [sessionCheckQueued, setSessionCheckQueued] = useState(false)
  const previousActive = useRef(active)
  const requestRef = useRef<AbortController | null>(null)
  const requestSequence = useRef(0)
  const targetSequence = useRef(0)
  const targetNameInputs = useRef(new Map<string, HTMLInputElement>())
  const pendingFocusTargetKey = useRef<string | null>(null)

  function selectPortfolio(portfolio: Portfolio | null) {
    setSelected(portfolio)
    setDraft(portfolio ? createTargetDraft(portfolio) : [])
    setDirty(false)
  }

  function clearPrivateState() {
    setPortfolios([])
    selectPortfolio(null)
    setName('')
  }

  function mayDiscardDraft(): boolean {
    return (
      !dirty ||
      window.confirm(
        'Há metas ainda não salvas. Deseja descartar o rascunho e continuar?',
      )
    )
  }

  function applyPortfolios(values: Portfolio[], preserveDraft: boolean) {
    setPortfolios(values)
    const next =
      values.find((item) => item.id === selected?.id) ?? values[0] ?? null
    if (!preserveDraft || !dirty || next?.id !== selected?.id)
      selectPortfolio(next)
  }

  async function operation(
    action: (signal: AbortSignal) => Promise<void>,
    preserveFeedback = false,
  ) {
    requestRef.current?.abort()
    const controller = new AbortController()
    requestRef.current = controller
    const sequence = ++requestSequence.current
    setPending(true)
    if (!preserveFeedback) {
      setError(null)
      setNotice('')
    }
    try {
      await action(controller.signal)
    } catch (failure) {
      if (controller.signal.aborted || sequence !== requestSequence.current)
        return
      const apiError =
        failure instanceof WalletApiError
          ? failure
          : new WalletApiError(
              0,
              'Não foi possível concluir a operação. Tente novamente.',
            )
      setError(apiError)
      setNotice('')
      if (apiError.status === 401) {
        clearPrivateState()
        setLogoutFailed(false)
        setSession((current) =>
          current ? { ...current, authenticated: false } : current,
        )
      }
    } finally {
      if (sequence === requestSequence.current && !controller.signal.aborted)
        setPending(false)
    }
  }

  async function refreshSession(signal: AbortSignal) {
    setVerifyingSession(true)
    try {
      const current = await readSession(signal)
      if (signal.aborted) return
      if (current.authenticated && current.walletAvailable) {
        const values = await readPortfolios(signal)
        if (signal.aborted) return
        const ownsSelection = values.some((item) => item.id === selected?.id)
        if (!ownsSelection) {
          setName('')
          setError(null)
          setNotice('')
        }
        applyPortfolios(values, ownsSelection)
      } else {
        clearPrivateState()
        setLogoutFailed(false)
        setError(null)
        setNotice('')
      }
      setSession(current)
    } catch (failure) {
      if (!signal.aborted) {
        clearPrivateState()
        setSession((current) =>
          current ? { ...current, authenticated: false } : current,
        )
      }
      throw failure
    } finally {
      if (!signal.aborted) setVerifyingSession(false)
    }
  }

  function logout() {
    clearPrivateState()
    setVerifyingSession(false)
    setSession((current) =>
      current ? { ...current, authenticated: false } : current,
    )
    void operation(async (signal) => {
      try {
        await endSession(signal)
        if (signal.aborted) return
        setLogoutFailed(false)
        setNotice(
          'Sessão encerrada. As carteiras continuam salvas no servidor.',
        )
      } catch (failure) {
        if (!signal.aborted) setLogoutFailed(true)
        throw failure
      }
    })
  }

  useEffect(() => {
    const controller = new AbortController()
    requestRef.current = controller
    const sequence = ++requestSequence.current
    void (async () => {
      try {
        const current = await readSession(controller.signal)
        if (controller.signal.aborted) return
        setSession(current)
        if (current.authenticated && current.walletAvailable) {
          const values = await readPortfolios(controller.signal)
          if (controller.signal.aborted) return
          setPortfolios(values)
          const first = values[0] ?? null
          setSelected(first)
          setDraft(first ? createTargetDraft(first) : [])
        }
      } catch (failure) {
        if (controller.signal.aborted) return
        if (failure instanceof WalletApiError && failure.status === 401) {
          setSession((current) =>
            current ? { ...current, authenticated: false } : current,
          )
          setPortfolios([])
          setSelected(null)
          setDraft([])
        }
        setError(
          failure instanceof WalletApiError
            ? failure
            : new WalletApiError(
                0,
                'Não foi possível carregar a carteira. Tente novamente.',
              ),
        )
      } finally {
        if (!controller.signal.aborted && sequence === requestSequence.current)
          setPending(false)
      }
    })()
    return () => {
      requestSequence.current += 1
      controller.abort()
      requestRef.current?.abort()
    }
  }, [])

  const canRecheckSession =
    session?.authenticated === true && session.walletAvailable && !logoutFailed

  useLayoutEffect(() => {
    const returning = active && !previousActive.current
    previousActive.current = active
    if (returning && canRecheckSession) setSessionCheckQueued(true)
  }, [active, canRecheckSession])

  useEffect(() => {
    if (!active || !canRecheckSession) return
    function checkOnVisibility() {
      if (document.visibilityState === 'visible') setSessionCheckQueued(true)
    }
    document.addEventListener('visibilitychange', checkOnVisibility)
    return () =>
      document.removeEventListener('visibilitychange', checkOnVisibility)
  }, [active, canRecheckSession])

  const recheckSession = useEffectEvent(() => {
    setSessionCheckQueued(false)
    if (canRecheckSession) void operation(refreshSession, true)
  })

  useEffect(() => {
    if (!active || !sessionCheckQueued || pending) return
    const scheduled = window.setTimeout(() => recheckSession(), 0)
    return () => window.clearTimeout(scheduled)
  }, [active, sessionCheckQueued, pending])

  useLayoutEffect(() => {
    const key = pendingFocusTargetKey.current
    if (key !== null) {
      pendingFocusTargetKey.current = null
      targetNameInputs.current.get(key)?.focus()
    }
  }, [draft])

  function addTarget() {
    const key = `new-${++targetSequence.current}`
    pendingFocusTargetKey.current = key
    setDraft((current) => [
      ...current,
      { key, id: null, name: '', percentage: '0' },
    ])
    setDirty(true)
    setNotice('')
  }

  function removeTarget(index: number, key: string) {
    pendingFocusTargetKey.current =
      draft[index + 1]?.key ?? draft[index - 1]?.key ?? null
    setDraft((current) => current.filter((target) => target.key !== key))
    setDirty(true)
    setNotice('')
  }

  function updateTarget(key: string, changes: Partial<TargetDraft>) {
    setDraft((current) =>
      current.map((target) =>
        target.key === key ? { ...target, ...changes } : target,
      ),
    )
    setDirty(true)
    setNotice('')
    setError(null)
  }

  const validationError = draft.length > 0 ? validateTargets(draft) : null
  const authenticated =
    canRecheckSession && !sessionCheckQueued && !verifyingSession

  return (
    <section
      className="saved-portfolio"
      aria-labelledby="wallet-title"
      aria-busy={pending || sessionCheckQueued}
    >
      <div className="wallet-heading">
        <div>
          <p className="eyebrow">Sua carteira</p>
          <h1 id="wallet-title">Comece pelas suas metas.</h1>
          <p>
            Crie uma carteira e salve a distribuição desejada entre classes. As
            metas ficam disponíveis quando você voltar.
          </p>
        </div>
        <span className="wallet-stage">01 / Carteiras e metas</span>
      </div>

      <p className="wallet-scope">
        Esta primeira etapa salva carteiras e metas em reais. Compras, vendas,
        posições e cotações serão acrescentadas nas próximas etapas. Use somente
        informações fictícias durante os testes.
      </p>

      {(pending || sessionCheckQueued) && (
        <p role="status" className="wallet-notice">
          {sessionCheckQueued || verifyingSession
            ? 'Verificando sua sessão…'
            : 'Aguarde, carregando sua carteira…'}
        </p>
      )}
      {error && (
        <div className="wallet-error" role="alert">
          <p>{error.message}</p>
          {dirty && (
            <p>As metas que você editou continuam no rascunho desta página.</p>
          )}
          {!logoutFailed &&
            (error.status === 403 ||
              error.status === 404 ||
              error.status === 0) && (
              <button
                type="button"
                disabled={pending}
                onClick={() => void operation(refreshSession)}
              >
                Verificar sessão e tentar novamente
              </button>
            )}
        </div>
      )}
      {notice && (
        <p role="status" className="wallet-notice">
          {notice}
        </p>
      )}

      {logoutFailed && (
        <div className="wallet-panel wallet-login">
          <h2>Confirme o encerramento da sessão.</h2>
          <p>
            Os dados foram ocultados desta tela, mas o servidor ainda não
            confirmou a saída. Tente novamente antes de sair do dispositivo.
          </p>
          <button type="button" disabled={pending} onClick={logout}>
            Tentar encerrar sessão novamente
          </button>
        </div>
      )}

      {!session && !pending && !error && (
        <p role="status">Verificando sessão…</p>
      )}

      {session && !session.walletAvailable && (
        <div className="wallet-panel wallet-login">
          <p className="eyebrow">Demonstração pública</p>
          <h2>Explore a demonstração sem criar uma conta.</h2>
          <p>
            Carteiras salvas ainda não estão habilitadas nesta versão. Para
            experimentar a persistência, use o ambiente local de carteira com
            banco de dados. As abas Visão geral, Ativos e Simulador continuam
            disponíveis.
          </p>
        </div>
      )}

      {session?.walletAvailable && !session.authenticated && !logoutFailed && (
        <div className="wallet-panel wallet-login">
          <p className="eyebrow">Acesso à carteira</p>
          <h2>Entre para salvar suas metas.</h2>
          {session.loginMode === 'local' ? (
            <>
              <p>
                Este ambiente de estudo usa um usuário fictício. Ele permite
                testar a carteira salva sem dados pessoais ou credenciais
                externas.
              </p>
              <button
                className="wallet-primary"
                type="button"
                disabled={pending}
                onClick={() =>
                  void operation(async (signal) => {
                    await startLocalSession(signal)
                    if (!signal.aborted) await refreshSession(signal)
                  })
                }
              >
                Entrar com usuário fictício
              </button>
            </>
          ) : session.loginMode === 'google' ? (
            <>
              <p>
                Use sua conta Google para acessar as carteiras vinculadas à sua
                sessão.
              </p>
              <a className="wallet-primary" href="/oauth2/authorization/google">
                Entrar com Google
              </a>
            </>
          ) : (
            <p>
              O acesso a carteiras salvas não está disponível neste ambiente.
            </p>
          )}
        </div>
      )}

      {authenticated && (
        <>
          <div className="wallet-session-bar">
            <p>
              Olá, <strong>{session.displayName ?? 'usuário'}</strong>
              <span>
                {session.loginMode === 'local'
                  ? ' · sessão fictícia de estudo'
                  : ' · sessão autenticada'}
              </span>
            </p>
            <button type="button" onClick={logout}>
              Sair
            </button>
          </div>

          <div className="wallet-grid">
            <aside className="wallet-panel" aria-labelledby="wallet-list-title">
              <p className="eyebrow">01 / Organizar</p>
              <h2 id="wallet-list-title">Minhas carteiras</h2>
              {portfolios.length === 0 ? (
                <p className="wallet-empty">
                  Nenhuma carteira salva ainda. Dê um nome ao seu primeiro
                  cenário.
                </p>
              ) : (
                <div className="wallet-list">
                  {portfolios.map((portfolio) => (
                    <button
                      className={
                        selected?.id === portfolio.id ? 'selected' : undefined
                      }
                      key={portfolio.id}
                      type="button"
                      aria-pressed={selected?.id === portfolio.id}
                      disabled={pending}
                      onClick={() => {
                        if (selected?.id === portfolio.id || !mayDiscardDraft())
                          return
                        selectPortfolio(portfolio)
                        setError(null)
                        setNotice('')
                      }}
                    >
                      <strong>{portfolio.name}</strong>
                      <span>{portfolio.targets.length} classes · BRL</span>
                    </button>
                  ))}
                </div>
              )}
              <form
                className="wallet-create"
                onSubmit={(event) => {
                  event.preventDefault()
                  const trimmed = name.trim()
                  if (!trimmed || trimmed.length > 100 || pending) return
                  if (!mayDiscardDraft()) return
                  void operation(async (signal) => {
                    const created = await createPortfolio(trimmed, signal)
                    if (signal.aborted) return
                    setPortfolios((current) => [...current, created])
                    selectPortfolio(created)
                    setName('')
                    setNotice(
                      'Carteira criada com metas iniciais. Ajuste-as ao seu cenário fictício.',
                    )
                  })
                }}
              >
                <label htmlFor="wallet-name">Nome da nova carteira</label>
                <input
                  id="wallet-name"
                  maxLength={100}
                  value={name}
                  disabled={pending || portfolios.length >= 100}
                  placeholder="Ex.: Cenário de longo prazo"
                  onChange={(event) => setName(event.target.value)}
                />
                <button
                  className="wallet-primary"
                  disabled={pending || !name.trim() || portfolios.length >= 100}
                  type="submit"
                >
                  Criar carteira
                </button>
                {portfolios.length >= 100 && (
                  <p>O limite deste ambiente é de 100 carteiras.</p>
                )}
              </form>
              {dirty && (
                <p className="wallet-hint">
                  Salve as metas antes de trocar de carteira. Você será avisado
                  antes de descartar o rascunho atual.
                </p>
              )}
            </aside>

            <div
              className="wallet-panel"
              aria-labelledby="wallet-targets-title"
            >
              <p className="eyebrow">02 / Planejar</p>
              <h2 id="wallet-targets-title">
                {selected
                  ? `Metas de ${selected.name}`
                  : 'Sua próxima distribuição'}
              </h2>
              {!selected ? (
                <p className="wallet-empty">
                  Crie uma carteira para definir o percentual desejado em cada
                  classe.
                </p>
              ) : (
                <form
                  className="wallet-targets"
                  onSubmit={(event) => {
                    event.preventDefault()
                    if (pending || validationError || !dirty) return
                    void operation(async (signal) => {
                      const saved = await saveTargets(
                        selected.id,
                        targetsRequest(selected, draft),
                        signal,
                      )
                      if (signal.aborted) return
                      setPortfolios((current) =>
                        current.map((portfolio) =>
                          portfolio.id === saved.id ? saved : portfolio,
                        ),
                      )
                      selectPortfolio(saved)
                      setNotice(
                        'Metas salvas. Elas estarão disponíveis ao abrir a carteira novamente.',
                      )
                    })
                  }}
                >
                  <p className="wallet-hint">
                    A soma precisa ser exatamente 100%. As classes e percentuais
                    são definidos por você.
                  </p>
                  <div className="wallet-target-list">
                    {draft.map((target, index) => (
                      <div className="wallet-target-row" key={target.key}>
                        <label>
                          <span>Classe {index + 1}</span>
                          <input
                            aria-label={`Nome da classe ${index + 1}`}
                            ref={(input) => {
                              if (input)
                                targetNameInputs.current.set(target.key, input)
                              else targetNameInputs.current.delete(target.key)
                            }}
                            maxLength={60}
                            disabled={pending}
                            value={target.name}
                            onChange={(event) =>
                              updateTarget(target.key, {
                                name: event.target.value,
                              })
                            }
                          />
                        </label>
                        <label>
                          <span>Meta (%)</span>
                          <input
                            aria-label={`Meta da classe ${index + 1}`}
                            inputMode="decimal"
                            disabled={pending}
                            value={target.percentage}
                            onChange={(event) =>
                              updateTarget(target.key, {
                                percentage: event.target.value,
                              })
                            }
                          />
                        </label>
                        <button
                          className="wallet-remove"
                          type="button"
                          aria-label={`Remover classe ${index + 1}`}
                          disabled={pending || draft.length <= 1}
                          onClick={() => removeTarget(index, target.key)}
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                  <button
                    type="button"
                    className="wallet-add"
                    disabled={pending || draft.length >= 20}
                    onClick={addTarget}
                  >
                    <span aria-hidden="true">+</span> Adicionar classe
                  </button>
                  <p
                    id="wallet-target-validation"
                    role="status"
                    aria-label="Validação das metas"
                    aria-atomic="true"
                    className={
                      validationError
                        ? 'wallet-validation invalid'
                        : 'wallet-validation'
                    }
                  >
                    {validationError ??
                      'Metas somam 100%. Distribuição pronta para salvar.'}
                  </p>
                  <div className="wallet-actions">
                    <button
                      className="wallet-primary"
                      type="submit"
                      aria-describedby="wallet-target-validation"
                      disabled={
                        pending ||
                        !!validationError ||
                        !dirty ||
                        error?.status === 409
                      }
                    >
                      Salvar metas
                    </button>
                    <button
                      type="button"
                      disabled={pending}
                      onClick={() =>
                        void operation(async (signal) => {
                          const values = await readPortfolios(signal)
                          if (signal.aborted) return
                          applyPortfolios(values, false)
                          setNotice(
                            'Carteira recarregada com as metas salvas no servidor.',
                          )
                        })
                      }
                    >
                      {dirty
                        ? 'Descartar rascunho e recarregar'
                        : 'Recarregar metas salvas'}
                    </button>
                  </div>
                  {error?.status === 409 && (
                    <p className="wallet-hint">
                      Confira seu rascunho antes de recarregar. A recarga
                      substitui estas metas pela versão salva mais recente.
                    </p>
                  )}
                  <p className="wallet-hint">
                    {dirty
                      ? 'Alterações ainda não salvas.'
                      : 'Metas sincronizadas com a carteira salva.'}
                  </p>
                </form>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  )
}
