import { CapaExibida } from '../capa-exibida';
import { TipoExibido } from '../tipo-exibido';

export interface LeituraExibida {
  readonly titulo: string;
  readonly detalhe: string | null;
  readonly noMinimo: boolean;
}

export interface TopoExibido {
  readonly nome: string;
  readonly tipo: TipoExibido;
  readonly rotulo: string;
  readonly capa: CapaExibida | null;
  readonly leitura: LeituraExibida | null;
}
