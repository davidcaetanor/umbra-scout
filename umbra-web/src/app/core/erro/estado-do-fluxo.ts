import { Observable, catchError, map, of, startWith } from 'rxjs';
import { EstadoDoBloco } from '../../shared/ui/estado-do-bloco/estado-do-bloco';
import { mensagemDoErro } from './mensagem-do-erro';

export function estadoDoFluxo<T>(
  fluxo: Observable<T>,
  estaVazio: (dados: T) => boolean = () => false,
): Observable<EstadoDoBloco<T>> {
  return fluxo.pipe(
    map((dados): EstadoDoBloco<T> => (estaVazio(dados) ? { tipo: 'vazio' } : { tipo: 'sucesso', dados })),
    catchError((erro: unknown) => {
      const mensagem = mensagemDoErro(erro);
      return of<EstadoDoBloco<T>>(mensagem === null ? { tipo: 'carregando' } : { tipo: 'erro', mensagem });
    }),
    startWith<EstadoDoBloco<T>>({ tipo: 'carregando' }),
  );
}
