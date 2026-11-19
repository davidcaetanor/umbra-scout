import { EstadoDoBloco } from '../../shared/ui/estado-do-bloco/estado-do-bloco';
import { mensagemDoErro } from './mensagem-do-erro';

export async function carregarEstado<T>(carga: () => Promise<T>, estaVazio: (dados: T) => boolean = () => false): Promise<EstadoDoBloco<T>> {
  try {
    const dados = await carga();
    return estaVazio(dados) ? { tipo: 'vazio' } : { tipo: 'sucesso', dados };
  } catch (erro) {
    const mensagem = mensagemDoErro(erro);
    return mensagem === null ? { tipo: 'carregando' } : { tipo: 'erro', mensagem };
  }
}
