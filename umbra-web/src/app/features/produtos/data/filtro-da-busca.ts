export type TipoDeProduto = 'JOGO' | 'HARDWARE';

export type CategoriaDeHardware =
  | 'CPU'
  | 'GPU'
  | 'PLACA_MAE'
  | 'MEMORIA_RAM'
  | 'FONTE'
  | 'SSD'
  | 'HD'
  | 'COOLER'
  | 'KIT_HARDWARE'
  | 'PLACA_INTERFACE';

export type OrdenacaoDaBusca = 'nome' | 'menorPreco';

export interface FiltroDaBusca {
  readonly termo: string | null;
  readonly tipo: TipoDeProduto | null;
  readonly categoria: CategoriaDeHardware | null;
  readonly ordenacao: OrdenacaoDaBusca;
  readonly pagina: number;
}
