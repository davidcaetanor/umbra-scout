const RAIZ_DO_MASCOTE = '/mascote';
const RAIZ_DA_MARCA = '/marca';

export type RetratoDoMascote = 'base' | 'confuso' | 'feliz' | 'surpreso' | 'tranquilo';

export const PASTA_DOS_SPRITES = `${RAIZ_DO_MASCOTE}/spritesheets`;
export const PASTA_DO_DESTAQUE = `${RAIZ_DO_MASCOTE}/destaque`;
export const MARCA_DO_CABECALHO = `${RAIZ_DA_MARCA}/umbra-cabeca.png`;

export function caminhoDoRetrato(retrato: RetratoDoMascote): string {
  return `${RAIZ_DO_MASCOTE}/retratos/${retrato}.png`;
}
