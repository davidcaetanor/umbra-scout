import { OfertaResponse } from '../../../api/models/oferta-response';
import { CapaExibida } from '../../../shared/capa/capa-exibida';
import { TipoExibido } from '../../../shared/capa/tipo-exibido';

export type SituacaoDoMelhorPreco = OfertaResponse['situacao'];

export interface MelhorPrecoListado {
  readonly valorCentavos: number;
  readonly loja: string;
  readonly gratuito: boolean;
  readonly situacao: SituacaoDoMelhorPreco;
  readonly expiraEm: number | null;
  readonly verificadoEm: number;
}

export interface MonitoramentoListado {
  readonly id: number;
  readonly produtoId: number;
  readonly nome: string;
  readonly capa: CapaExibida | null;
  readonly tipo: TipoExibido;
  readonly precoAlvoCentavos: number;
  readonly ativo: boolean;
  readonly melhorPreco: MelhorPrecoListado | null;
  readonly distanciaDoAlvoCentavos: number | null;
  readonly alvoAtingido: boolean;
  readonly foraDeVenda: boolean;
}
