import { TestBed } from '@angular/core/testing';
import { CHAVE_DA_PROXIMA_PIADA, PIADAS, RodizioDePiadas, alvoExorbitante } from './piadas-do-alvo';

describe('piadas do alvo exorbitante', () => {
  beforeEach(() => localStorage.clear());

  it('só considera exorbitante acima de R$ 5.000,00', () => {
    expect(alvoExorbitante(500_000)).toBe(false);
    expect(alvoExorbitante(500_001)).toBe(true);
  });

  it('alterna as piadas em ordem e recomeça depois da última', () => {
    const rodizio = TestBed.inject(RodizioDePiadas);

    const vistas = Array.from({ length: PIADAS.length + 1 }, () => rodizio.proxima().texto);

    expect(new Set(vistas.slice(0, PIADAS.length)).size).toBe(PIADAS.length);
    expect(vistas.at(-1)).toBe(vistas[0]);
  });

  it('continua de onde parou entre visitas e ignora valor estragado', () => {
    localStorage.setItem(CHAVE_DA_PROXIMA_PIADA, '3');
    expect(TestBed.inject(RodizioDePiadas).proxima()).toBe(PIADAS[3]);
    expect(localStorage.getItem(CHAVE_DA_PROXIMA_PIADA)).toBe('4');

    localStorage.setItem(CHAVE_DA_PROXIMA_PIADA, 'abc');
    expect(TestBed.inject(RodizioDePiadas).proxima()).toBe(PIADAS[4]);
  });
});
