export type PeriodoDoHistorico = '30d' | '90d' | '1a';

export interface PontoDoHistorico {
  readonly instante: number;
  readonly valorCentavos: number | null;
}

export interface SerieDaLoja {
  readonly loja: string;
  readonly pontos: readonly PontoDoHistorico[];
}

export interface MinimoDoHistorico {
  readonly valorCentavos: number;
  readonly loja: string;
  readonly em: number;
}

export interface HistoricoDoProduto {
  readonly periodo: PeriodoDoHistorico;
  readonly consultadoEm: number;
  readonly observadoDesde: number | null;
  readonly melhorPreco: readonly PontoDoHistorico[];
  readonly porLoja: readonly SerieDaLoja[];
  readonly minimo: MinimoDoHistorico | null;
}
