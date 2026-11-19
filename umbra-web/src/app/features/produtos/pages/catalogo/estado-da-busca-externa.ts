import { ProdutoExterno } from '../../data/produto-externo';

export type EstadoDaBuscaExterna =
  | { readonly tipo: 'inativa' }
  | { readonly tipo: 'buscando' }
  | { readonly tipo: 'concluida'; readonly produtos: readonly ProdutoExterno[] }
  | { readonly tipo: 'falhou'; readonly mensagem: string };
