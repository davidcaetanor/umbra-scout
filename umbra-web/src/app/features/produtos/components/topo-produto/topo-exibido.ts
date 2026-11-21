import { CapaExibida } from '../capa-exibida';
import { TipoExibido } from '../tipo-exibido';

export type TomDaLeitura = 'bom' | 'costume' | 'mais-barato';

export interface LeituraExibida {
  readonly titulo: string;
  readonly detalhe: string;
  readonly tom: TomDaLeitura;
}

export interface TopoExibido {
  readonly nome: string;
  readonly tipo: TipoExibido;
  readonly rotulo: string;
  readonly capa: CapaExibida | null;
  readonly leitura: LeituraExibida | null;
}
