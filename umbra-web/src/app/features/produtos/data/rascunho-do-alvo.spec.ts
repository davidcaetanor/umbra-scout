import { TestBed } from '@angular/core/testing';
import { CHAVE_DO_RASCUNHO, RascunhoDoAlvo, rascunhoDoJogoExterno, rascunhoDoProduto } from './rascunho-do-alvo';

describe('RascunhoDoAlvo', () => {
  let rascunho: RascunhoDoAlvo;

  beforeEach(() => {
    sessionStorage.clear();
    rascunho = TestBed.inject(RascunhoDoAlvo);
  });

  it('devolve o alvo guardado uma vez só, para o mesmo produto', () => {
    rascunho.guardar(rascunhoDoProduto(42), 14990);

    expect(rascunho.retomar(rascunhoDoProduto(42))).toBe(14990);
    expect(rascunho.retomar(rascunhoDoProduto(42))).toBeNull();
  });

  it('guarda tambem o alvo de um jogo de fora do catalogo, pelo gid', () => {
    rascunho.guardar(rascunhoDoJogoExterno('018d937f'), 6000);

    expect(rascunho.retomar(rascunhoDoProduto(42))).toBeNull();
    expect(rascunho.retomar(rascunhoDoJogoExterno('018d937f'))).toBe(6000);
  });

  it('não entrega o rascunho de outro produto nem o apaga', () => {
    rascunho.guardar(rascunhoDoProduto(42), 14990);

    expect(rascunho.retomar(rascunhoDoProduto(7))).toBeNull();
    expect(sessionStorage.getItem(CHAVE_DO_RASCUNHO)).not.toBeNull();
  });

  it('ainda le o formato antigo, guardado so com o produtoId', () => {
    sessionStorage.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ produtoId: 42, alvoCentavos: 14990 }));

    expect(rascunho.retomar(rascunhoDoProduto(42))).toBe(14990);
  });

  it('ignora conteúdo estragado', () => {
    sessionStorage.setItem(CHAVE_DO_RASCUNHO, '{quebrado');
    expect(rascunho.retomar(rascunhoDoProduto(42))).toBeNull();

    sessionStorage.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ produtoId: 42, alvoCentavos: -1 }));
    expect(rascunho.retomar(rascunhoDoProduto(42))).toBeNull();
  });
});
