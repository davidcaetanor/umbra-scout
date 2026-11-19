export interface PoseMascote {
  readonly arquivo: string;
  readonly larguraQuadro: number;
  readonly alturaQuadro: number;
  readonly sobraAbaixoDosPes: number;
  readonly quadros: number;
  readonly quadroDeRepouso: number;
  readonly fps: number;
  readonly repeticao: 'loop' | 'uma-vez';
}
