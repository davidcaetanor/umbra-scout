import { Observable, firstValueFrom, of, throwError, toArray } from 'rxjs';
import { EstadoDoBloco } from '../../shared/ui/estado-do-bloco/estado-do-bloco';
import { ErroDaApi } from './erro-da-api';
import { estadoDoFluxo } from './estado-do-fluxo';

function estados<T>(fluxo: Observable<T>, estaVazio?: (dados: T) => boolean): Promise<EstadoDoBloco<T>[]> {
  return firstValueFrom(estadoDoFluxo(fluxo, estaVazio).pipe(toArray()));
}

describe('estadoDoFluxo', () => {
  it('comeca carregando e termina em sucesso', async () => {
    expect(await estados(of([1]))).toEqual([{ tipo: 'carregando' }, { tipo: 'sucesso', dados: [1] }]);
  });

  it('dado vazio pela regra da tela vira vazio', async () => {
    expect((await estados(of<number[]>([]), (lista) => lista.length === 0)).at(-1)).toEqual({ tipo: 'vazio' });
  });

  it('falha vira erro legivel e nao derruba o fluxo', async () => {
    const final = (await estados(throwError(() => new ErroDaApi({ tipo: 'rede' })))).at(-1);

    expect(final).toEqual({ tipo: 'erro', mensagem: expect.stringContaining('Não conseguimos falar') });
  });

  it('sessao encerrada continua carregando ate o login assumir', async () => {
    const final = (await estados(throwError(() => new ErroDaApi({ tipo: 'sessao-encerrada' })))).at(-1);

    expect(final).toEqual({ tipo: 'carregando' });
  });
});
