import { DirecaoMascote, EstadoMascote, TamanhoMascote } from './estado-mascote';
import { PoseMascote } from './pose-mascote';

const PASTA = 'mascote/spritesheets';

function pose(
  nome: string,
  largura: number,
  sobraAbaixoDosPes: number,
  quadros: number,
  fps: number,
  repeticao: PoseMascote['repeticao'],
): PoseMascote {
  return {
    arquivo: `${PASTA}/${nome}.png`,
    larguraQuadro: largura,
    alturaQuadro: largura,
    sobraAbaixoDosPes,
    quadros,
    fps,
    repeticao,
  };
}

export const ESCALA_POR_TAMANHO: Readonly<Record<TamanhoMascote, number>> = {
  pequeno: 2,
  grande: 3,
};

export const POSES: Readonly<Record<EstadoMascote, Readonly<Partial<Record<DirecaoMascote, PoseMascote>>>>> = {
  observando: { south: pose('observando-south', 92, 14, 9, 6, 'loop') },
  buscando: {
    south: pose('buscando-south', 92, 14, 9, 8, 'loop'),
    east: pose('buscando-east', 88, 13, 9, 8, 'loop'),
    north: pose('buscando-north', 84, 12, 9, 8, 'loop'),
    west: pose('buscando-west', 88, 13, 9, 8, 'loop'),
  },
  digitando: { south: pose('digitando-south', 92, 14, 9, 8, 'loop') },
  comemorando: { south: pose('comemorando-south', 92, 14, 9, 10, 'uma-vez') },
  erro: { south: pose('erro-south', 92, 14, 9, 8, 'uma-vez') },
  perdido: { south: pose('perdido-south', 92, 14, 9, 8, 'uma-vez') },
  pausado: { south: pose('pausado-south', 92, 14, 9, 4, 'loop') },
  acenando: { south: pose('acenando-south', 92, 14, 9, 8, 'uma-vez') },
};

export function poseDe(estado: EstadoMascote, direcao: DirecaoMascote): PoseMascote {
  const poses = POSES[estado];
  const escolhida = poses[direcao] ?? poses.south;
  if (!escolhida) {
    throw new Error(`Mascote sem pose para ${estado}`);
  }
  return escolhida;
}
