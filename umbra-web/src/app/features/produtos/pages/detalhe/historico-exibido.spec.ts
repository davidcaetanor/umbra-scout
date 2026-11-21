import { HistoricoDoProduto } from '../../data/historico-do-produto';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import {
  ContextoDoGrafico,
  DIA_MS,
  degraus,
  diasObservados,
  graficoExibido,
  legendaExibida,
  lojasIniciais,
  minimoExibido,
  opcoesDePeriodo,
  periodoEfetivo,
  temHistorico,
} from './historico-exibido';

const FIM = Date.parse('2026-11-21T12:00:00Z');
const dia = (n: number) => FIM - n * DIA_MS;

const HISTORICO: HistoricoDoProduto = {
  periodo: '90d',
  consultadoEm: FIM,
  observadoDesde: dia(120),
  melhorPreco: [
    { instante: dia(80), valorCentavos: 5000 },
    { instante: dia(60), valorCentavos: 3000 },
    { instante: dia(50), valorCentavos: 4000 },
    { instante: dia(20), valorCentavos: 3000 },
    { instante: dia(10), valorCentavos: 4500 },
  ],
  porLoja: [
    { loja: 'Steam', pontos: [{ instante: dia(80), valorCentavos: 5000 }] },
    { loja: 'GOG', pontos: [{ instante: dia(80), valorCentavos: 6000 }] },
    { loja: 'Nuuvem', pontos: [{ instante: dia(80), valorCentavos: 7000 }] },
  ],
  minimo: { valorCentavos: 3000, loja: 'Steam', em: dia(20) },
};

const OFERTA: OfertaDaLoja = {
  loja: 'GOG',
  valorCentavos: 4500,
  preco: { atual: 'R$ 45,00', original: null, descontoPct: null, loja: 'GOG' },
  gratuito: false,
  endereco: 'https://loja.exemplo',
  coletadoEm: '2026-11-21T10:00:00Z',
  expiraEm: null,
};

function contexto(parcial: Partial<ContextoDoGrafico> = {}): ContextoDoGrafico {
  return { historico: HISTORICO, periodo: '90d', melhorOferta: OFERTA, ofertas: [OFERTA], alvoCentavos: null, lojas: null, ...parcial };
}

describe('períodos do histórico', () => {
  it('conta os dias inteiros desde o início da observação', () => {
    expect(diasObservados(HISTORICO)).toBe(120);
    expect(diasObservados({ ...HISTORICO, observadoDesde: null })).toBe(0);
  });

  it('libera cada período só quando há mais dias que o período anterior', () => {
    const opcoes = opcoesDePeriodo(30);

    expect(opcoes.map((opcao) => opcao.disponivel)).toEqual([true, false, false]);
    expect(opcoes[1].aviso).toBe('disponível a partir de 31 dias observados');
    expect(opcoes[2].aviso).toBe('disponível a partir de 91 dias observados');
    expect(opcoesDePeriodo(91).every((opcao) => opcao.disponivel)).toBe(true);
  });

  it('troca o período escolhido pelo maior disponível', () => {
    expect(periodoEfetivo('90d', opcoesDePeriodo(10))).toBe('30d');
    expect(periodoEfetivo('1a', opcoesDePeriodo(40))).toBe('90d');
    expect(periodoEfetivo('90d', opcoesDePeriodo(40))).toBe('90d');
  });

  it('reconhece histórico sem nenhum preço', () => {
    expect(temHistorico(HISTORICO)).toBe(true);
    expect(temHistorico({ ...HISTORICO, melhorPreco: [{ instante: dia(3), valorCentavos: null }] })).toBe(false);
  });
});

describe('degraus', () => {
  it('estende o último valor até a consulta', () => {
    expect(degraus([{ instante: dia(2), valorCentavos: 100 }], FIM)).toEqual([
      { x: dia(2), y: 100 },
      { x: FIM, y: 100 },
    ]);
  });

  it('segura o valor até o início da lacuna e termina nela', () => {
    const pontos = degraus(
      [
        { instante: dia(5), valorCentavos: 100 },
        { instante: dia(3), valorCentavos: null },
      ],
      FIM,
    );

    expect(pontos).toEqual([
      { x: dia(5), y: 100 },
      { x: dia(3), y: 100 },
      { x: dia(3), y: null },
    ]);
  });
});

describe('menor registrado', () => {
  it('destaca todos os trechos no valor mínimo e põe o losango no trecho do mínimo informado', () => {
    const minimo = minimoExibido(HISTORICO.melhorPreco, { ...HISTORICO.minimo!, em: dia(55) }, dia(90), FIM);

    expect(minimo.trechos.filter((ponto) => ponto.y === null)).toHaveLength(2);
    expect(minimo.losango).toEqual({ x: (dia(60) + dia(50)) / 2, y: 3000 });
  });

  it('usa o trecho mais recente quando o instante do mínimo não cai em nenhum', () => {
    const minimo = minimoExibido(HISTORICO.melhorPreco, { ...HISTORICO.minimo!, em: dia(200) }, dia(90), FIM);

    expect(minimo.losango?.x).toBe((dia(20) + dia(10)) / 2);
  });

  it('corta o trecho no início do eixo e fica sem losango se o mínimo está fora do período', () => {
    const cortado = minimoExibido(HISTORICO.melhorPreco, HISTORICO.minimo!, dia(55), FIM);
    expect(cortado.trechos[0]).toEqual({ x: dia(55), y: 3000 });

    const fora = minimoExibido(HISTORICO.melhorPreco, { ...HISTORICO.minimo!, valorCentavos: 1000 }, dia(90), FIM);
    expect(fora.trechos).toEqual([]);
    expect(fora.losango).toBeNull();
  });
});

describe('gráfico exibido', () => {
  it('começa no período ou na observação, o que for mais tarde', () => {
    expect(graficoExibido(contexto()).inicio).toBe(dia(90));
    expect(graficoExibido(contexto({ periodo: '1a' })).inicio).toBe(dia(120));
  });

  it('marca hoje com a melhor oferta e o alvo de quem monitora', () => {
    const grafico = graficoExibido(contexto({ alvoCentavos: 3500 }));

    expect(grafico.hoje).toEqual({ x: FIM, y: 4500 });
    expect(grafico.alvo).toBe(3500);
    expect(grafico.resumo).toContain('alvo de busca R$ 35,00');
  });

  it('fica sem ponto de hoje quando nenhuma loja tem o produto', () => {
    const grafico = graficoExibido(contexto({ melhorOferta: null }));

    expect(grafico.hoje).toBeNull();
    expect(grafico.resumo).toContain('hoje indisponível nas lojas');
  });

  it('por loja mostra só as lojas escolhidas, sem o mínimo', () => {
    const grafico = graficoExibido(contexto({ lojas: ['GOG', 'Nuuvem'] }));

    expect(grafico.linhas.map((linha) => [linha.rotulo, linha.cor])).toEqual([
      ['GOG', 'historico-1'],
      ['Nuuvem', 'historico-3'],
    ]);
    expect(grafico.minimo).toBeNull();
  });

  it('começa por loja com as duas primeiras lojas', () => {
    expect(lojasIniciais(HISTORICO)).toEqual(['Steam', 'GOG']);
  });
});

describe('legenda', () => {
  it('traz hoje, melhor preço do dia, menor registrado e alvo', () => {
    const celulas = legendaExibida(contexto({ alvoCentavos: 3500 }));

    expect(celulas.map((celula) => celula.marca)).toEqual(['hoje', 'linha-1', 'minimo', 'alvo']);
    expect(celulas[0]).toMatchObject({ titulo: 'Hoje, 21/11', valor: 'R$ 45,00', detalhe: 'GOG' });
    expect(celulas[1]).toMatchObject({ titulo: 'Melhor preço do dia', detalhe: 'entre as lojas' });
    expect(celulas[2]).toMatchObject({ valor: 'R$ 30,00', detalhe: 'Steam, 01/11/2026' });
    expect(celulas[3]).toMatchObject({ titulo: 'Alvo de busca', valor: 'R$ 35,00', detalhe: null });
  });

  it('diz que hoje está indisponível e mostra mínimo grátis', () => {
    const celulas = legendaExibida(
      contexto({ melhorOferta: null, historico: { ...HISTORICO, minimo: { valorCentavos: 0, loja: 'Epic', em: dia(5) } } }),
    );

    expect(celulas[0]).toMatchObject({ valor: null, detalhe: 'Indisponível nas lojas' });
    expect(celulas[2].valor).toBe('Grátis');
  });

  it('por loja troca a linha pelas lojas e tira o menor registrado', () => {
    const celulas = legendaExibida(contexto({ lojas: ['Steam', 'GOG'] }));

    expect(celulas.map((celula) => [celula.marca, celula.titulo])).toEqual([
      ['hoje', 'Hoje, 21/11'],
      ['linha-1', 'Steam'],
      ['linha-3', 'GOG'],
    ]);
  });

  describe('preço de hoje por loja', () => {
    const steam = { ...OFERTA, loja: 'Steam', valorCentavos: 9989, preco: { ...OFERTA.preco, atual: 'R$ 99,89', loja: 'Steam' } };
    const gog = { ...OFERTA, loja: 'GOG', valorCentavos: 49950, preco: { ...OFERTA.preco, atual: 'R$ 499,50', loja: 'GOG' } };
    const lojasDa = (ofertas: OfertaDaLoja[], lojas = ['Steam', 'GOG']) =>
      legendaExibida(contexto({ lojas, ofertas })).filter((celula) => celula.marca !== 'hoje').map(({ valor, detalhe }) => [valor, detalhe]);

    it('mostra o preço de cada uma e a diferença na mais cara', () => {
      expect(lojasDa([steam, gog])).toEqual([
        ['R$ 99,89', 'a mais barata das duas'],
        ['R$ 499,50', 'R$ 399,61 acima da Steam'],
      ]);
    });

    it('preço igual e loja sem oferta hoje', () => {
      expect(lojasDa([steam, { ...gog, valorCentavos: 9989 }])).toEqual([
        ['R$ 99,89', 'mesmo preço da outra loja'],
        ['R$ 499,50', 'mesmo preço da outra loja'],
      ]);
      expect(lojasDa([steam])).toEqual([
        ['R$ 99,89', 'única com oferta hoje'],
        [null, 'Indisponível hoje'],
      ]);
    });

    it('uma loja só mostra o preço sem comparação', () => {
      expect(lojasDa([steam, gog], ['GOG'])).toEqual([['R$ 499,50', null]]);
    });
  });

  it('alvo acima de duas vezes o maior preço sai da escala e a legenda explica', () => {
    const longe = contexto({ alvoCentavos: 10_000_000 });

    expect(graficoExibido(longe).alvo).toBeNull();
    expect(legendaExibida(longe).at(-1)).toMatchObject({ marca: 'alvo', valor: 'R$ 100.000,00', detalhe: 'acima da escala do gráfico' });
    expect(graficoExibido(contexto({ alvoCentavos: 10_000 })).alvo).toBe(10_000);
    expect(graficoExibido(contexto({ alvoCentavos: 10_001 })).alvo).toBeNull();
  });

  it('por loja mede a escala só pelas lojas marcadas', () => {
    expect(graficoExibido(contexto({ lojas: ['Steam'], alvoCentavos: 10_000 })).alvo).toBe(10_000);
    expect(graficoExibido(contexto({ lojas: ['Steam'], alvoCentavos: 10_001 })).alvo).toBeNull();
    expect(graficoExibido(contexto({ lojas: ['Nuuvem'], alvoCentavos: 14_000 })).alvo).toBe(14_000);
  });
});
