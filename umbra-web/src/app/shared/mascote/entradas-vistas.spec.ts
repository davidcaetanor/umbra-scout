import { TestBed } from '@angular/core/testing';
import { EntradasVistas } from './entradas-vistas';

describe('EntradasVistas', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('lembra a entrada na mesma sessão, entre instâncias', () => {
    TestBed.inject(EntradasVistas).registrar('/catalogo#observando');
    TestBed.resetTestingModule();

    expect(TestBed.inject(EntradasVistas).jaViu('/catalogo#observando')).toBe(true);
    expect(TestBed.inject(EntradasVistas).jaViu('/monitoramentos#pausado')).toBe(false);
  });

  it('ignora conteúdo corrompido no armazenamento', () => {
    sessionStorage.setItem('umbra.mascote.entradas-vistas', '{quebrado');
    expect(TestBed.inject(EntradasVistas).jaViu('qualquer')).toBe(false);
  });

  it('segue funcionando em memória quando o armazenamento é bloqueado', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const entradas = TestBed.inject(EntradasVistas);
    entradas.registrar('/catalogo#observando');
    expect(entradas.jaViu('/catalogo#observando')).toBe(true);
  });
});
