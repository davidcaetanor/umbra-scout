import { DirecaoMascote, EstadoMascote, TamanhoMascote } from './estado-mascote';
import { PoseMascote } from './pose-mascote';

const PASTA = 'mascote/spritesheets';
const QUADROS = 9;

interface DefinicaoDePose {
  readonly nome: string;
  readonly largura: number;
  readonly sobraAbaixoDosPes: number;
  readonly quadroDeRepouso: number;
  readonly fps: number;
  readonly repeticao: PoseMascote['repeticao'];
}

function pose(definicao: DefinicaoDePose): PoseMascote {
  return {
    arquivo: `${PASTA}/${definicao.nome}.png`,
    larguraQuadro: definicao.largura,
    alturaQuadro: definicao.largura,
    sobraAbaixoDosPes: definicao.sobraAbaixoDosPes,
    quadros: QUADROS,
    quadroDeRepouso: definicao.quadroDeRepouso,
    fps: definicao.fps,
    repeticao: definicao.repeticao,
  };
}

export const ESCALA_POR_TAMANHO: Readonly<Record<TamanhoMascote, number>> = {
  pequeno: 2,
  grande: 3,
};

const UMA_VEZ = 'uma-vez';
const LOOP = 'loop';

export const POSES: Readonly<Record<EstadoMascote, Readonly<Partial<Record<DirecaoMascote, PoseMascote>>>>> = {
  observando: {
    south: pose({ nome: 'observando-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 4, fps: 6, repeticao: UMA_VEZ }),
  },
  buscando: {
    south: pose({ nome: 'buscando-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 0, fps: 8, repeticao: LOOP }),
    east: pose({ nome: 'buscando-east', largura: 88, sobraAbaixoDosPes: 13, quadroDeRepouso: 0, fps: 8, repeticao: LOOP }),
    north: pose({ nome: 'buscando-north', largura: 84, sobraAbaixoDosPes: 12, quadroDeRepouso: 0, fps: 8, repeticao: LOOP }),
    west: pose({ nome: 'buscando-west', largura: 88, sobraAbaixoDosPes: 13, quadroDeRepouso: 0, fps: 8, repeticao: LOOP }),
  },
  digitando: {
    south: pose({ nome: 'digitando-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 4, fps: 8, repeticao: LOOP }),
  },
  comemorando: {
    south: pose({ nome: 'comemorando-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 6, fps: 10, repeticao: UMA_VEZ }),
  },
  erro: {
    south: pose({ nome: 'erro-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 5, fps: 8, repeticao: UMA_VEZ }),
  },
  perdido: {
    south: pose({ nome: 'perdido-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 6, fps: 8, repeticao: UMA_VEZ }),
  },
  pausado: {
    south: pose({ nome: 'pausado-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 5, fps: 4, repeticao: UMA_VEZ }),
  },
  acenando: {
    south: pose({ nome: 'acenando-south', largura: 92, sobraAbaixoDosPes: 14, quadroDeRepouso: 7, fps: 8, repeticao: UMA_VEZ }),
  },
};

export function poseDe(estado: EstadoMascote, direcao: DirecaoMascote): PoseMascote {
  const poses = POSES[estado];
  const escolhida = poses[direcao] ?? poses.south;
  if (!escolhida) {
    throw new Error(`Mascote sem pose para ${estado}`);
  }
  return escolhida;
}
