import './App.css'
import { ContributionSimulator } from './features/contribution-simulator/ContributionSimulator'

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

function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="brand" href="#top" aria-label="Ir para o início">
          <BrandMark />
          <span>
            <strong>Planejador</strong>
            <small>de Carteira</small>
          </span>
        </a>
        <span className="status-badge">Módulo inicial</span>
      </header>

      <main id="top">
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
              Simulação educacional. Não constitui recomendação de investimento.
            </p>
          </div>
        </section>

        <ContributionSimulator />
      </main>

      <footer>
        <p>
          Valores fictícios para estudo · Regras financeiras calculadas no
          backend
        </p>
        <p>Projeto pessoal de Thiago Jose</p>
      </footer>
    </div>
  )
}

export default App
