import { CapaExibida } from './capa-exibida';
import { PrecoExibido } from './preco-exibido';
import { TipoExibido } from './tipo-exibido';

export interface ProdutoExibido {
  readonly nome: string;
  readonly tipo: TipoExibido;
  readonly capa: CapaExibida | null;
  readonly categoria: string | null;
  readonly preco: PrecoExibido | null;
}
