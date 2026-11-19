import { DirecaoMascote, EstadoMascote } from './estado-mascote';
import { POSES, poseDe } from './poses';

const ESTADOS: readonly EstadoMascote[] = [
  'observando',
  'buscando',
  'digitando',
  'comemorando',
  'erro',
  'perdido',
  'pausado',
  'acenando',
  'ocioso',
];

describe('POSES', () => {
  it('cobre os nove estados do componente com a vista de frente', () => {
    expect(Object.keys(POSES).sort()).toEqual([...ESTADOS].sort());
    ESTADOS.forEach((estado) => expect(POSES[estado].south).toBeDefined());
  });

  it('dá à caminhada as quatro direções', () => {
    const direcoes: readonly DirecaoMascote[] = ['south', 'east', 'north', 'west'];
    direcoes.forEach((direcao) => expect(poseDe('buscando', direcao).arquivo).toContain(`buscando-${direcao}`));
  });

  it('mantém quadro quadrado e sobra abaixo dos pés pequena', () => {
    Object.values(POSES)
      .flatMap((poses) => Object.values(poses))
      .forEach((pose) => {
        expect(pose.larguraQuadro).toBe(pose.alturaQuadro);
        expect(pose.sobraAbaixoDosPes).toBeLessThan(pose.alturaQuadro / 4);
      });
  });
});
