import { CapaExibida } from '../../../shared/capa/capa-exibida';
import { TipoExibido } from '../../../shared/capa/tipo-exibido';

export type SituacaoExibida = 'vigente' | 'promocao-no-prazo' | 'verificacao-atrasada' | 'promocao-vencida-sem-coleta';

export interface MelhorPrecoExibido {
  readonly valorCentavos: number;
  readonly loja: string;
  readonly gratuito: boolean;
  readonly situacao: SituacaoExibida;
  readonly expiraEm: number | null;
  readonly verificadoEm: number;
}

export interface MonitoramentoExibido {
  readonly id: number;
  readonly produtoId: number;
  readonly nome: string;
  readonly capa: CapaExibida | null;
  readonly tipo: TipoExibido;
  readonly precoAlvoCentavos: number;
  readonly ativo: boolean;
  readonly melhorPreco: MelhorPrecoExibido | null;
  readonly distanciaDoAlvoCentavos: number | null;
  readonly alvoAtingido: boolean;
  readonly foraDeVenda: boolean;
}
