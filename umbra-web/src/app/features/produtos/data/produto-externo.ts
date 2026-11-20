import { CapaDoProduto } from './capa-do-produto';
import { PrecoDoProduto } from './preco-do-produto';

export interface ProdutoExterno {
  readonly gid: string;
  readonly nome: string;
  readonly capa: CapaDoProduto | null;
  readonly preco: PrecoDoProduto;
  readonly enderecoDaLoja: string;
}
