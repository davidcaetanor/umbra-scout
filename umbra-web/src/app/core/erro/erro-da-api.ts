import { CasoDeErro } from './caso-de-erro';

export class ErroDaApi extends Error {
  constructor(readonly caso: CasoDeErro) {
    super(`Falha na API: ${caso.tipo}`);
    this.name = 'ErroDaApi';
  }
}

export function casoDe(erro: unknown): CasoDeErro {
  return erro instanceof ErroDaApi ? erro.caso : { tipo: 'inesperado' };
}
