import { CapaDoProduto } from '../../../shared/capa/capa-do-produto';
import { PrecoDoProduto } from './preco-do-produto';

export interface ProdutoExterno {
  readonly gid: string;
  readonly nome: string;
  readonly capa: CapaDoProduto | null;
  readonly preco: PrecoDoProduto;
  readonly precoCentavos: number;
  readonly gratuito: boolean;
  readonly enderecoDaLoja: string;
}
