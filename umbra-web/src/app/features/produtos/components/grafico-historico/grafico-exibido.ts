export interface PontoExibido {
  readonly x: number;
  readonly y: number | null;
}

export type CorDaLinha = 'historico-1' | 'historico-3';

export interface LinhaExibida {
  readonly rotulo: string;
  readonly cor: CorDaLinha;
  readonly pontos: readonly PontoExibido[];
}

export interface MinimoExibido {
  readonly valor: number;
  readonly trechos: readonly PontoExibido[];
  readonly losango: PontoExibido | null;
}

export interface GraficoExibido {
  readonly inicio: number;
  readonly fim: number;
  readonly linhas: readonly LinhaExibida[];
  readonly minimo: MinimoExibido | null;
  readonly hoje: PontoExibido | null;
  readonly alvo: number | null;
  readonly resumo: string;
}
