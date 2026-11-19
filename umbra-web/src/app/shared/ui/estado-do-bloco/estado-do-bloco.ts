export type EstadoDoBloco<T> =
  | { readonly tipo: 'carregando' }
  | { readonly tipo: 'vazio' }
  | { readonly tipo: 'erro'; readonly mensagem: string }
  | { readonly tipo: 'sucesso'; readonly dados: T };

export function dadosDe<T>(estado: EstadoDoBloco<T>): T | null {
  return estado.tipo === 'sucesso' ? estado.dados : null;
}
