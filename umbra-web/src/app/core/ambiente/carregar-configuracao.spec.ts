import { carregarConfiguracao } from './carregar-configuracao';
import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';

function respostaCom(status: number, corpo: unknown): typeof fetch {
  return vi.fn().mockResolvedValue(new Response(JSON.stringify(corpo), { status }));
}

describe('carregarConfiguracao', () => {
  it('busca o config.json sem cache e valida o conteudo', async () => {
    const buscar = respostaCom(200, { apiUrl: 'http://localhost:8080', loginGoogle: true });

    const configuracao = await carregarConfiguracao(buscar);

    expect(buscar).toHaveBeenCalledWith('/config.json', { cache: 'no-store' });
    expect(configuracao.apiUrl).toBe('http://localhost:8080');
  });

  it('falha com resposta de erro do servidor', async () => {
    await expect(carregarConfiguracao(respostaCom(404, {}))).rejects.toBeInstanceOf(ConfiguracaoInvalidaError);
  });
});
