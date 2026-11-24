import { CapaExibida } from '../../../shared/capa/capa-exibida';
import { PrecoExibido } from './preco-exibido';

export interface ProdutoExternoExibido {
  readonly nome: string;
  readonly capa: CapaExibida | null;
  readonly preco: PrecoExibido;
  readonly gratuito: boolean;
  readonly enderecoDaLoja: string;
}

export type MonitoramentoDoExterno =
  | { readonly tipo: 'monitorando'; readonly produtoId: number; readonly alvo: string }
  | { readonly tipo: 'ja-monitorado' };
