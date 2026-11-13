import { useLayoutEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import './App.css'
import { AssetExplorer } from './features/asset-explorer/AssetExplorer'
import { ContributionSimulator } from './features/contribution-simulator/ContributionSimulator'
import type { SimulationDraftPreset } from './features/contribution-simulator/contracts'
import { PortfolioOverview } from './features/portfolio-overview/PortfolioOverview'
import { createDemoSimulationPreset } from './features/portfolio-overview/demoPortfolio'
import { SavedPortfolio } from './features/saved-portfolio/SavedPortfolio'

const NAV_ITEMS = [
  { id: 'overview', label: 'Visão geral' },
  { id: 'assets', label: 'Ativos' },
  { id: 'simulator', label: 'Simulador' },
  { id: 'wallet', label: 'Carteira' },
] as const

type ActiveView = (typeof NAV_ITEMS)[number]['id']
type ColorTheme = 'light' | 'dark'

const THEME_STORAGE_KEY = 'portfolio-planner-theme'

function getLoginResult() {
  const result = new URLSearchParams(window.location.search).get('login')
  return result === 'success' || result === 'failed' ? result : null
}

function getInitialTheme(): ColorTheme {
  try {
    const savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
    if (savedTheme === 'light' || savedTheme === 'dark') {
      return savedTheme
    }
  } catch {
    // A preferência continua funcional durante a sessão se o storage falhar.
  }

  return typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}

function BrandMark() {
  return (
    <svg
      aria-hidden="true"
      className="brand-mark"
      viewBox="0 0 40 40"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M8 29V19M16 29V12M24 29V21M32 29V8" />
      <path d="m7 15 9-7 8 8 9-11" />
    </svg>
  )
}

function ThemeIcon({ theme }: { theme: ColorTheme }) {
  const nextTheme = theme === 'light' ? 'dark' : 'light'

  return (
    <span aria-hidden="true" className="theme-icon" data-next-theme={nextTheme}>
      <svg className="theme-icon-moon" viewBox="0 0 24 24">
        <path d="M20.2 15.1A8.4 8.4 0 0 1 8.9 3.8 8.5 8.5 0 1 0 20.2 15.1Z" />
      </svg>
      <svg className="theme-icon-sun" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="3.5" />
        <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
      </svg>
    </span>
  )
}

function ViewIcon({ view }: { view: ActiveView }) {
  return (
    <svg aria-hidden="true" className="nav-icon" viewBox="0 0 24 24">
      {view === 'overview' ? (
        <path d="M4 19V10h4v9M10 19V5h4v14M16 19v-7h4v7M3 19h18" />
      ) : view === 'assets' ? (
        <>
          <rect x="3" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="3" width="7" height="7" rx="1.5" />
          <rect x="3" y="14" width="7" height="7" rx="1.5" />
          <rect x="14" y="14" width="7" height="7" rx="1.5" />
        </>
      ) : view === 'wallet' ? (
        <>
          <path d="M4 7V5h14v3M4 8h16v12H4z" />
          <path d="M15 12h5v5h-5z" />
        </>
      ) : (
        <>
          <path d="M3 6h18M3 12h18M3 18h18" />
          <circle cx="8" cy="6" r="2" />
          <circle cx="16" cy="12" r="2" />
          <circle cx="10" cy="18" r="2" />
        </>
      )}
    </svg>
  )
}

function App() {
  const [loginResult] = useState(getLoginResult)
  const [activeView, setActiveView] = useState<ActiveView>(
    loginResult ? 'wallet' : 'overview',
  )
  const [theme, setTheme] = useState<ColorTheme>(getInitialTheme)
  const [walletVisited, setWalletVisited] = useState(loginResult !== null)
  const [demoTransfer, setDemoTransfer] = useState<{
    version: number
    preset: SimulationDraftPreset
  } | null>(null)
  const [assetTransfer, setAssetTransfer] = useState<{
    version: number
    categoryId: string
  } | null>(null)
  const mainRef = useRef<HTMLElement>(null)
  const previousView = useRef(activeView)

  useLayoutEffect(() => {
    if (previousView.current === activeView) return
    previousView.current = activeView
    mainRef.current?.focus({ preventScroll: true })
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    // Keep the new page at the top after the browser settles its scroll anchor.
    const frame = window.requestAnimationFrame(() => {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    })
    return () => window.cancelAnimationFrame(frame)
  }, [activeView])

  useLayoutEffect(() => {
    if (!loginResult) return
    const url = new URL(window.location.href)
    url.searchParams.delete('login')
    window.history.replaceState(window.history.state, '', url)
  }, [loginResult])

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
    document
      .querySelector<HTMLMetaElement>('meta[name="theme-color"]')
      ?.setAttribute('content', theme === 'dark' ? '#0c1715' : '#f4f6f1')

    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, theme)
    } catch {
      // O tema aplicado não depende da persistência da preferência.
    }
  }, [theme])

  function openView(view: ActiveView) {
    if (view === 'wallet') setWalletVisited(true)
    if (view === activeView) {
      mainRef.current?.focus({ preventScroll: true })
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
      return
    }
    setActiveView(view)
  }

  function useDemoPortfolio() {
    setDemoTransfer((current) => ({
      version: (current?.version ?? 0) + 1,
      preset: createDemoSimulationPreset(),
    }))
    openView('simulator')
  }

  function exploreAssets(categoryId: string) {
    setAssetTransfer((current) => ({
      version: (current?.version ?? 0) + 1,
      categoryId,
    }))
    openView('assets')
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <a
          className="brand"
          href="#top"
          aria-label="Abrir visão geral"
          onClick={(event) => {
            event.preventDefault()
            openView('overview')
          }}
        >
          <BrandMark />
          <span>
            <strong>Planejador</strong>
            <small>de Carteira</small>
          </span>
        </a>
        <nav
          aria-label="Navegação principal"
          className="app-nav"
          data-active-view={activeView}
          style={
            {
              '--nav-active-index': NAV_ITEMS.findIndex(
                (item) => item.id === activeView,
              ),
            } as CSSProperties
          }
        >
          <span aria-hidden="true" className="nav-indicator" />
          {NAV_ITEMS.map((item) => (
            <button
              aria-current={activeView === item.id ? 'page' : undefined}
              className={activeView === item.id ? 'active' : undefined}
              key={item.id}
              type="button"
              onClick={() => openView(item.id)}
            >
              <ViewIcon view={item.id} />
              {item.label}
            </button>
          ))}
        </nav>
        <div className="header-tools">
          <button
            className="theme-toggle"
            type="button"
            onClick={() =>
              setTheme((currentTheme) =>
                currentTheme === 'light' ? 'dark' : 'light',
              )
            }
            aria-label={`Ativar modo ${theme === 'light' ? 'escuro' : 'claro'}`}
          >
            <ThemeIcon theme={theme} />
            <span>Modo {theme === 'light' ? 'escuro' : 'claro'}</span>
          </button>
          <span className="status-badge">
            {activeView === 'simulator'
              ? 'Módulo inicial'
              : activeView === 'wallet'
                ? 'Carteiras e metas'
                : 'Dados sintéticos'}
          </span>
        </div>
      </header>

      <main id="top" ref={mainRef} tabIndex={-1}>
        <div className="app-view" hidden={activeView !== 'overview'}>
          <PortfolioOverview
            onOpenSimulator={() => openView('simulator')}
            onUseDemoPortfolio={useDemoPortfolio}
            onExploreAssets={exploreAssets}
          />
        </div>
        <div className="app-view" hidden={activeView !== 'assets'}>
          <AssetExplorer
            key={assetTransfer?.version ?? 0}
            initialCategoryId={assetTransfer?.categoryId}
          />
        </div>
        <div className="app-view" hidden={activeView !== 'simulator'}>
          <section className="intro" aria-labelledby="page-title">
            <div>
              <p className="eyebrow">Simulador de aportes</p>
              <h1 id="page-title">
                Simule como um novo aporte pode aproximar a carteira das metas.
              </h1>
            </div>
            <div className="intro-copy">
              <p>
                Este é o primeiro módulo de uma plataforma de carteira em
                evolução e usa um cenário informado na tela. A aba Carteira
                permite salvar classes e metas no ambiente local habilitado;
                cadastro de ativos e movimentações ainda não estão disponíveis.
              </p>
              <p className="disclaimer">
                Simulação educacional, sem recomendação de investimento. Use
                apenas valores fictícios: as entradas são enviadas à API para o
                cálculo e não são salvas em uma carteira.
              </p>
            </div>
          </section>

          <ContributionSimulator
            key={demoTransfer?.version ?? 0}
            initialPreset={demoTransfer?.preset}
          />
        </div>
        <div className="app-view" hidden={activeView !== 'wallet'}>
          {loginResult === 'failed' && (
            <p className="wallet-error" role="alert">
              Não foi possível entrar com o Google. Tente novamente pela aba
              Carteira.
            </p>
          )}
          {walletVisited && <SavedPortfolio active={activeView === 'wallet'} />}
        </div>
      </main>

      <footer>
        <p>Dados sintéticos para estudo · Simulador calculado no backend</p>
        <p>Projeto pessoal de Thiago Jose</p>
      </footer>
    </div>
  )
}

export default App
