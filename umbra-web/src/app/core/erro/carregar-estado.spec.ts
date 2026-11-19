import { carregarEstado } from './carregar-estado';
import { ErroDaApi } from './erro-da-api';

describe('carregarEstado', () => {
  it('dado presente vira sucesso', async () => {
    expect(await carregarEstado(() => Promise.resolve([1]))).toEqual({ tipo: 'sucesso', dados: [1] });
  });

  it('dado que a tela considera vazio vira vazio, diferente de erro', async () => {
    expect(await carregarEstado(() => Promise.resolve([]), (lista) => lista.length === 0)).toEqual({ tipo: 'vazio' });
  });

  it('falha vira erro com a mensagem legivel do caso', async () => {
    const estado = await carregarEstado(() => Promise.reject(new ErroDaApi({ tipo: 'tempo-esgotado' })));

    expect(estado).toEqual({ tipo: 'erro', mensagem: expect.stringContaining('demorou demais') });
  });

  it('sessao encerrada segue carregando, sem piscar mensagem antes do login', async () => {
    const estado = await carregarEstado(() => Promise.reject(new ErroDaApi({ tipo: 'sessao-encerrada' })));

    expect(estado).toEqual({ tipo: 'carregando' });
  });
});
