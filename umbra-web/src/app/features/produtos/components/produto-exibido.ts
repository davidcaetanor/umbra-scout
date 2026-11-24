import { CapaExibida } from '../../../shared/capa/capa-exibida';
import { PrecoExibido } from './preco-exibido';
import { TipoExibido } from '../../../shared/capa/tipo-exibido';

export interface ProdutoExibido {
  readonly nome: string;
  readonly tipo: TipoExibido;
  readonly capa: CapaExibida | null;
  readonly categoria: string | null;
  readonly preco: PrecoExibido | null;
}
