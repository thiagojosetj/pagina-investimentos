import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import App from './App'

afterEach(cleanup)

describe('App', () => {
  it('identifies the simulator as the initial module of a broader platform', () => {
    render(<App />)

    expect(screen.getByText('Módulo inicial')).toBeInTheDocument()
    expect(
      screen.getByText(/primeiro módulo de uma plataforma de carteira/i),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/carteiras salvas, ativos e movimentações ainda não/i),
    ).toBeInTheDocument()
  })
})
