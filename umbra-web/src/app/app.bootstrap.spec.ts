import { ApplicationRef } from '@angular/core';
import { DependenciasDaPartida, MENSAGEM_DE_FALHA_NA_PARTIDA, iniciarAplicacao } from './app.bootstrap';
import { ConfiguracaoInvalidaError } from './core/ambiente';

const CONFIGURACAO = { apiUrl: 'http://localhost:8080', loginGoogle: true };

function dependencias(sobrescritas: Partial<DependenciasDaPartida>): DependenciasDaPartida {
  return {
    carregar: () => Promise.resolve(CONFIGURACAO),
    iniciar: () => Promise.resolve({} as ApplicationRef),
    corpo: document.createElement('body'),
    ...sobrescritas,
  };
}

describe('iniciarAplicacao', () => {
  beforeEach(() => vi.spyOn(console, 'error').mockImplementation(() => undefined));

  it('inicia a aplicacao com a configuracao carregada', async () => {
    const iniciar = vi.fn().mockResolvedValue({} as ApplicationRef);

    await iniciarAplicacao(dependencias({ iniciar }));

    expect(iniciar).toHaveBeenCalledWith(CONFIGURACAO);
  });

  it('mostra mensagem legivel quando a configuracao falha, sem iniciar a aplicacao', async () => {
    const iniciar = vi.fn();
    const corpo = document.createElement('body');

    await iniciarAplicacao(
      dependencias({ carregar: () => Promise.reject(new ConfiguracaoInvalidaError('teste')), iniciar, corpo }),
    );

    expect(iniciar).not.toHaveBeenCalled();
    expect(corpo.textContent).toBe(MENSAGEM_DE_FALHA_NA_PARTIDA);
  });

  it('mostra a mesma mensagem quando a partida do Angular falha', async () => {
    const corpo = document.createElement('body');

    await iniciarAplicacao(dependencias({ iniciar: () => Promise.reject(new Error('falha')), corpo }));

    expect(corpo.textContent).toBe(MENSAGEM_DE_FALHA_NA_PARTIDA);
  });
});

describe('iniciarAplicacao com as dependencias do navegador', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.body.innerHTML = '';
  });

  it('busca o config.json e monta a aplicacao no elemento raiz', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(CONFIGURACAO))));
    document.body.innerHTML = '<umbra-root></umbra-root>';

    await iniciarAplicacao();

    expect(fetch).toHaveBeenCalledWith('/config.json', { cache: 'no-store' });
    expect(document.querySelector('umbra-root main router-outlet')).not.toBeNull();
  });

  it('escreve a mensagem de falha no corpo do documento', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 500 })));

    await iniciarAplicacao();

    expect(document.body.textContent).toBe(MENSAGEM_DE_FALHA_NA_PARTIDA);
  });
});
