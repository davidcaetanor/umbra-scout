import { TestBed } from '@angular/core/testing';
import { FAIXAS_DE_PIADAS, PREFIXO_DA_PROXIMA_PIADA, RodizioDePiadas, faixaDoAlvo } from './piadas-do-alvo';

describe('piadas do alvo por faixa', () => {
  beforeEach(() => localStorage.clear());

  it.each([
    [500_000, null],
    [500_001, 'caro-mas-existe'],
    [2_000_000, 'caro-mas-existe'],
    [2_000_001, 'nao-e-peca'],
    [10_000_000, 'nao-e-peca'],
    [10_000_001, 'delirio'],
    [15_000_000, 'delirio'],
  ])('alvo de %d centavos cai na faixa %s', (centavos, nome) => {
    expect(faixaDoAlvo(centavos)?.nome ?? null).toBe(nome);
  });

  it('cada faixa tem ao menos três piadas para o rodízio', () => {
    FAIXAS_DE_PIADAS.forEach((faixa) => expect(faixa.piadas.length).toBeGreaterThanOrEqual(3));
  });

  it('alterna dentro da faixa e recomeça depois da última', () => {
    const rodizio = TestBed.inject(RodizioDePiadas);
    const faixa = FAIXAS_DE_PIADAS[0];

    const vistas = Array.from({ length: faixa.piadas.length + 1 }, () => rodizio.proxima(faixa).texto);

    expect(new Set(vistas.slice(0, faixa.piadas.length)).size).toBe(faixa.piadas.length);
    expect(vistas.at(-1)).toBe(vistas[0]);
  });

  it('cada faixa guarda a sua vez sem atrapalhar a outra', () => {
    const rodizio = TestBed.inject(RodizioDePiadas);
    const [delirio, naoEPeca] = FAIXAS_DE_PIADAS;

    rodizio.proxima(delirio);
    expect(rodizio.proxima(naoEPeca)).toBe(naoEPeca.piadas[0]);
    expect(rodizio.proxima(delirio)).toBe(delirio.piadas[1]);
    expect(localStorage.getItem(PREFIXO_DA_PROXIMA_PIADA + delirio.nome)).toBe('2');
  });

  it('continua de onde parou entre visitas e ignora valor estragado', () => {
    const faixa = FAIXAS_DE_PIADAS[2];
    localStorage.setItem(PREFIXO_DA_PROXIMA_PIADA + faixa.nome, '2');
    expect(TestBed.inject(RodizioDePiadas).proxima(faixa)).toBe(faixa.piadas[2]);

    localStorage.setItem(PREFIXO_DA_PROXIMA_PIADA + faixa.nome, 'abc');
    expect(TestBed.inject(RodizioDePiadas).proxima(faixa)).toBe(faixa.piadas[0]);
  });
});
