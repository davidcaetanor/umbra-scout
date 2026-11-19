import { ErroDeCampo } from './erro-de-campo';

export type CasoDeErro =
  | { readonly tipo: 'rede' }
  | { readonly tipo: 'tempo-esgotado' }
  | { readonly tipo: 'sessao-encerrada' }
  | { readonly tipo: 'credencial-recusada' }
  | { readonly tipo: 'acesso-negado' }
  | { readonly tipo: 'nao-encontrado' }
  | { readonly tipo: 'invalido'; readonly campos: readonly ErroDeCampo[] }
  | { readonly tipo: 'conflito'; readonly detalhe: string | null }
  | { readonly tipo: 'regra'; readonly detalhe: string | null }
  | { readonly tipo: 'indisponivel'; readonly tentarDeNovoEmSegundos: number | null }
  | { readonly tipo: 'inesperado' };

export type TipoDeErro = CasoDeErro['tipo'];
