import { PrecoDoProduto } from './preco-do-produto';

export interface ProdutoExterno {
  readonly gid: string;
  readonly nome: string;
  readonly imagem: string | null;
  readonly preco: PrecoDoProduto;
  readonly enderecoDaLoja: string;
}
