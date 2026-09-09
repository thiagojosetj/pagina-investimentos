import { useLayoutEffect, useRef, useState } from 'react'
import './App.css'
import { ContributionSimulator } from './features/contribution-simulator/ContributionSimulator'
import { PortfolioOverview } from './features/portfolio-overview/PortfolioOverview'

type ActiveView = 'overview' | 'simulator'
type ColorTheme = 'light' | 'dark'

const THEME_STORAGE_KEY = 'portfolio-planner-theme'

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
  return theme === 'light' ? (
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <path d="M20.2 15.1A8.4 8.4 0 0 1 8.9 3.8 8.5 8.5 0 1 0 20.2 15.1Z" />
    </svg>
  ) : (
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="3.5" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  )
}

function App() {
  const [activeView, setActiveView] = useState<ActiveView>('overview')
  const [theme, setTheme] = useState<ColorTheme>(getInitialTheme)
  const mainRef = useRef<HTMLElement>(null)

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
    setActiveView(view)
    window.requestAnimationFrame(() => {
      const main = mainRef.current
      const prefersReducedMotion =
        typeof window.matchMedia === 'function' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches

      main?.focus({ preventScroll: true })
      main?.scrollIntoView?.({
        behavior: prefersReducedMotion ? 'auto' : 'smooth',
      })
    })
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
        <nav className="app-nav" aria-label="Navegação principal">
          <button
            aria-current={activeView === 'overview' ? 'page' : undefined}
            className={activeView === 'overview' ? 'active' : undefined}
            type="button"
            onClick={() => openView('overview')}
          >
            Visão geral
          </button>
          <button
            aria-current={activeView === 'simulator' ? 'page' : undefined}
            className={activeView === 'simulator' ? 'active' : undefined}
            type="button"
            onClick={() => openView('simulator')}
          >
            Simulador
          </button>
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
            {activeView === 'overview' ? 'Dados sintéticos' : 'Módulo inicial'}
          </span>
        </div>
      </header>

      <main id="top" ref={mainRef} tabIndex={-1}>
        <div hidden={activeView !== 'overview'}>
          <PortfolioOverview onOpenSimulator={() => openView('simulator')} />
        </div>
        <div hidden={activeView !== 'simulator'}>
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
                evolução. Por enquanto, ele trabalha com um cenário informado na
                tela; carteiras salvas, ativos e movimentações ainda não estão
                disponíveis.
              </p>
              <p className="disclaimer">
                Simulação educacional. Não constitui recomendação de
                investimento.
              </p>
            </div>
          </section>

          <ContributionSimulator />
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
