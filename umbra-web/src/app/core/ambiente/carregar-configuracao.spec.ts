import { carregarConfiguracao } from './carregar-configuracao';
import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';

function respostaCom(status: number, corpo: unknown): typeof fetch {
  return vi.fn().mockResolvedValue(new Response(JSON.stringify(corpo), { status }));
}

function documentoCom(html: string): Document {
  return new DOMParser().parseFromString(`<html><head>${html}</head><body></body></html>`, 'text/html');
}

const SEM_BLOCO = documentoCom('');

describe('carregarConfiguracao', () => {
  it('usa o bloco embutido sem buscar o config.json', async () => {
    const buscar = respostaCom(200, {});
    const documento = documentoCom(
      '<script id="configuracao" type="application/json">{"apiUrl":"https://api.exemplo.com","loginGoogle":false}</script>',
    );

    const configuracao = await carregarConfiguracao(documento, buscar);

    expect(buscar).not.toHaveBeenCalled();
    expect(configuracao.apiUrl).toBe('https://api.exemplo.com');
  });

  it('busca o config.json sem cache quando nao ha bloco embutido', async () => {
    const buscar = respostaCom(200, { apiUrl: 'http://localhost:8080', loginGoogle: true });

    const configuracao = await carregarConfiguracao(SEM_BLOCO, buscar);

    expect(buscar).toHaveBeenCalledWith('/config.json', { cache: 'no-store' });
    expect(configuracao.apiUrl).toBe('http://localhost:8080');
  });

  it('falha com resposta de erro do servidor', async () => {
    await expect(carregarConfiguracao(SEM_BLOCO, respostaCom(404, {}))).rejects.toBeInstanceOf(
      ConfiguracaoInvalidaError,
    );
  });
});
