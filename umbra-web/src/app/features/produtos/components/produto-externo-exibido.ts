import { CapaExibida } from '../../../shared/capa/capa-exibida';
import { PrecoExibido } from './preco-exibido';

export interface ProdutoExternoExibido {
  readonly nome: string;
  readonly capa: CapaExibida | null;
  readonly preco: PrecoExibido;
  readonly enderecoDaLoja: string;
}
