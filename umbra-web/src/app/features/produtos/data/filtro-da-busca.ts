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

export type OrdenacaoDaBusca = 'nome' | 'menorPreco' | 'melhoresOfertas' | 'maiorDesconto';

export type CodigoDaLoja = 'STEAM' | 'NUUVEM' | 'EPIC' | 'GOG' | 'GREEN_MAN_GAMING' | 'KABUM' | 'TERABYTE';

export const ORDENACOES_SO_DE_JOGO: readonly OrdenacaoDaBusca[] = ['melhoresOfertas', 'maiorDesconto'];

export const DESCONTOS_MINIMOS: readonly number[] = [20, 50, 75];

export interface FiltroDaBusca {
  readonly termo: string | null;
  readonly tipo: TipoDeProduto | null;
  readonly categoria: CategoriaDeHardware | null;
  readonly ordenacao: OrdenacaoDaBusca;
  readonly precoMin: number | null;
  readonly precoMax: number | null;
  readonly descontoMin: number | null;
  readonly loja: CodigoDaLoja | null;
  readonly pagina: number;
}
