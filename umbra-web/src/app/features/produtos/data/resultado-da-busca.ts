import { ProdutoDoCatalogo } from './produto-do-catalogo';

export interface ResultadoDaBusca {
  readonly produtos: readonly ProdutoDoCatalogo[];
  readonly pagina: number;
  readonly totalDePaginas: number;
  readonly total: number;
}
