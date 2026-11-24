import { OfertaResponse } from '../../../api/models/oferta-response';
import { CapaDoProduto } from '../../../shared/capa/capa-do-produto';
import { TipoDeProduto } from './filtro-da-busca';
import { PrecoDoProduto } from './preco-do-produto';

export interface OfertaDaLoja {
  readonly loja: string;
  readonly valorCentavos: number;
  readonly preco: PrecoDoProduto;
  readonly gratuito: boolean;
  readonly endereco: string;
  readonly coletadoEm: string;
  readonly expiraEm: number | null;
  readonly verificadoEm: number;
  readonly situacao: OfertaResponse['situacao'];
}

export interface ProdutoDetalhado {
  readonly id: number;
  readonly nome: string;
  readonly tipo: TipoDeProduto;
  readonly capa: CapaDoProduto | null;
  readonly categoria: string | null;
  readonly melhorOferta: OfertaDaLoja | null;
  readonly ofertas: readonly OfertaDaLoja[];
}
