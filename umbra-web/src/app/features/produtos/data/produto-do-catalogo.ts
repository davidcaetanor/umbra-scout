import { PrecoDoProduto } from './preco-do-produto';

export interface ProdutoDoCatalogo {
  readonly id: number;
  readonly nome: string;
  readonly imagem: string | null;
  readonly categoria: string | null;
  readonly preco: PrecoDoProduto | null;
}
