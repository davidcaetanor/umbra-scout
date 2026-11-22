import { TestBed } from '@angular/core/testing';
import { CHAVE_DO_RASCUNHO, RascunhoDoAlvo } from './rascunho-do-alvo';

describe('RascunhoDoAlvo', () => {
  let rascunho: RascunhoDoAlvo;

  beforeEach(() => {
    sessionStorage.clear();
    rascunho = TestBed.inject(RascunhoDoAlvo);
  });

  it('devolve o alvo guardado uma vez só, para o mesmo produto', () => {
    rascunho.guardar(42, 14990);

    expect(rascunho.retomar(42)).toBe(14990);
    expect(rascunho.retomar(42)).toBeNull();
  });

  it('não entrega o rascunho de outro produto nem o apaga', () => {
    rascunho.guardar(42, 14990);

    expect(rascunho.retomar(7)).toBeNull();
    expect(sessionStorage.getItem(CHAVE_DO_RASCUNHO)).not.toBeNull();
  });

  it('ignora conteúdo estragado', () => {
    sessionStorage.setItem(CHAVE_DO_RASCUNHO, '{quebrado');
    expect(rascunho.retomar(42)).toBeNull();

    sessionStorage.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ produtoId: 42, alvoCentavos: -1 }));
    expect(rascunho.retomar(42)).toBeNull();
  });
});
