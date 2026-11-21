import { configuracaoDoGrafico, rotuloSemRepetir } from './configuracao-do-grafico';
import { CoresDoTema } from './cores-do-tema';
import { GraficoExibido } from './grafico-exibido';

const CORES: CoresDoTema = {
  linha1: 'rgb(1, 1, 1)',
  linha3: 'rgb(3, 3, 3)',
  hoje: 'rgb(4, 4, 4)',
  minimo: 'rgb(5, 5, 5)',
  apoio: 'rgb(6, 6, 6)',
  alvo: 'rgb(7, 7, 7)',
  grade: 'rgb(8, 8, 8)',
  rotulo: 'rgb(9, 9, 9)',
  fonte: 'IBM Plex Mono',
};

const GRAFICO: GraficoExibido = {
  inicio: Date.parse('2026-11-01T00:00:00Z'),
  fim: Date.parse('2026-11-21T00:00:00Z'),
  linhas: [
    { rotulo: 'Steam', cor: 'historico-1', pontos: [{ x: 1, y: 100 }] },
    { rotulo: 'GOG', cor: 'historico-3', pontos: [{ x: 1, y: 200 }] },
  ],
  minimo: { valor: 100, trechos: [{ x: 1, y: 100 }], losango: { x: 2, y: 100 } },
  hoje: { x: 3, y: 150 },
  alvo: 90,
  resumo: 'resumo',
};

describe('configuração do gráfico', () => {
  it('desenha as linhas em degraus com as cores do tema e a segunda tracejada', () => {
    const [primeira, segunda] = configuracaoDoGrafico(GRAFICO, CORES).data.datasets;

    expect(primeira).toMatchObject({ label: 'Steam', stepped: true, borderColor: CORES.linha1, borderDash: [], pointHoverBackgroundColor: CORES.linha1 });
    expect(segunda).toMatchObject({ label: 'GOG', borderColor: CORES.linha3, borderDash: [6, 4] });
  });

  it('acrescenta apoio, mínimo, losango, alvo e hoje, com raio próprio nos pontos', () => {
    const conjuntos = configuracaoDoGrafico(GRAFICO, CORES).data.datasets;
    const porRotulo = Object.fromEntries(conjuntos.map((conjunto) => [conjunto.label, conjunto]));

    expect(Object.keys(porRotulo)).toEqual([
      'Steam',
      'GOG',
      'Apoio do menor registrado',
      'Menor registrado',
      'Losango do menor registrado',
      'Alvo de busca',
      'Hoje',
    ]);
    expect(porRotulo['Losango do menor registrado']).toMatchObject({ pointStyle: 'rectRot', pointBackgroundColor: CORES.minimo });
    expect(porRotulo['Hoje']).toMatchObject({ pointBackgroundColor: CORES.hoje });
    expect(Number(porRotulo['Hoje'].pointRadius)).toBeGreaterThan(0);
    expect(porRotulo['Alvo de busca'].data).toEqual([
      { x: GRAFICO.inicio, y: 90 },
      { x: GRAFICO.fim, y: 90 },
    ]);
  });

  it('omite as marcas que não existem', () => {
    const conjuntos = configuracaoDoGrafico({ ...GRAFICO, minimo: null, alvo: null, hoje: null }, CORES).data.datasets;

    expect(conjuntos.map((conjunto) => conjunto.label)).toEqual(['Steam', 'GOG']);
  });

  it('formata os eixos em datas e reais, e a dica só nas linhas', () => {
    const opcoes = configuracaoDoGrafico(GRAFICO, CORES).options!;
    const eixoX = opcoes.scales!['x']!;
    const eixoY = opcoes.scales!['y']!;
    const formatar = (eixo: typeof eixoX, valor: number) =>
      (eixo.ticks!.callback as (this: unknown, valor: number, indice: number, marcas: unknown[]) => string).call(null, valor, 0, []);

    expect(formatar(eixoX, Date.parse('2026-11-10T15:00:00Z'))).toBe('10/11');
    expect(formatar(eixoY, 14990)).toBe('R$ 149,90');
    const filtro = opcoes.plugins!.tooltip!.filter as (item: { datasetIndex: number }) => boolean;
    expect(filtro({ datasetIndex: 1 })).toBe(true);
    expect(filtro({ datasetIndex: 2 })).toBe(false);
  });

  it('não repete a mesma data no eixo', () => {
    const dia = Date.parse('2026-11-21T03:00:00Z');
    expect(rotuloSemRepetir(dia, undefined)).toBe('21/11');
    expect(rotuloSemRepetir(dia + 3_600_000, dia)).toBeNull();
    expect(rotuloSemRepetir(dia + 86_400_000, dia)).toBe('22/11');
  });

  it('não deixa o eixo de preço passar abaixo de zero', () => {
    const eixoY = configuracaoDoGrafico(GRAFICO, CORES).options!.scales!['y']! as { afterDataLimits: (eixo: { min: number }) => void };
    const negativo = { min: -500 };
    const positivo = { min: 300 };

    eixoY.afterDataLimits(negativo);
    eixoY.afterDataLimits(positivo);

    expect(negativo.min).toBe(0);
    expect(positivo.min).toBe(300);
  });

  it('a dica usa a cor da linha', () => {
    const dica = configuracaoDoGrafico(GRAFICO, CORES).options!.plugins!.tooltip!.callbacks!;
    const cor = (dica.labelColor as (item: unknown) => { backgroundColor: string }).call(null, { dataset: { borderColor: CORES.linha3 } });

    expect(cor.backgroundColor).toBe(CORES.linha3);
  });
});
