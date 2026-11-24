import { CapaDoProduto } from '../../../shared/capa/capa-do-produto';
import { TipoDeProduto } from './filtro-da-busca';
import { PrecoDoProduto } from './preco-do-produto';

export interface ProdutoDoCatalogo {
  readonly id: number;
  readonly nome: string;
  readonly tipo: TipoDeProduto;
  readonly capa: CapaDoProduto | null;
  readonly categoria: string | null;
  readonly preco: PrecoDoProduto | null;
}
