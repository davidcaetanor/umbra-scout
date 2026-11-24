import { HistoricoDoProduto, PontoDoHistorico } from '../../data/historico-do-produto';
import { DIA_MS } from './historico-exibido';
import {
  MAXIMO_DE_MUDANCAS_NA_LISTA,
  fraseSemGrafico,
  lojasQueMudaram,
  mostraGrafico,
  mudancas,
  mudancasPorLoja,
  verificacao,
} from './mudancas-do-historico';

const FIM = Date.parse('2026-11-21T15:00:00Z');
const ponto = (dias: number, valorCentavos: number | null): PontoDoHistorico => ({ instante: FIM - dias * DIA_MS, valorCentavos });

const HISTORICO: HistoricoDoProduto = {
  periodo: '30d',
  consultadoEm: FIM,
  observadoDesde: FIM - 20 * DIA_MS,
  melhorPreco: [ponto(20, 9000)],
  porLoja: [],
  minimo: null,
};

describe('mudanças do histórico', () => {
  it('conta só as trocas de valor, inclusive para indisponível', () => {
    expect(mudancas([ponto(10, 100), ponto(8, 100), ponto(5, 200), ponto(2, null)])).toEqual([
      { instante: FIM - 5 * DIA_MS, de: 100, para: 200 },
      { instante: FIM - 2 * DIA_MS, de: 200, para: null },
    ]);
  });

  it('mostra o gráfico a partir de duas mudanças', () => {
    expect(mostraGrafico([ponto(10, 100), ponto(5, 200)])).toBe(false);
    expect(mostraGrafico([ponto(10, 100), ponto(5, 200), ponto(2, 100)])).toBe(true);
  });

  it('separa as lojas que mudaram de preço', () => {
    expect(
      lojasQueMudaram([
        { loja: 'Steam', pontos: [ponto(10, 100), ponto(5, 90)] },
        { loja: 'GOG', pontos: [ponto(10, 100)] },
      ]),
    ).toEqual(['Steam']);
  });

  it('lista as mudanças mais recentes primeiro, com sentido, percentual e grátis', () => {
    const lista = mudancasPorLoja([
      { loja: 'Steam', pontos: [ponto(10, 30000), ponto(3, 7500), ponto(1, 0)] },
      { loja: 'GOG', pontos: [ponto(10, 7500), ponto(3, 29900), ponto(2, null), ponto(1, 8000)] },
    ]);

    expect(lista.map(({ data, texto }) => `${data} ${texto}`)).toEqual([
      '20/11 GOG voltou com R$ 80,00',
      '20/11 Steam caiu para Grátis (-100%)',
      '19/11 GOG ficou indisponível',
      '18/11 GOG subiu para R$ 299,00 (+299%)',
      '18/11 Steam caiu para R$ 75,00 (-75%)',
    ]);
  });

  it('limita a lista', () => {
    const muitos = Array.from({ length: 8 }, (_, posicao) => ponto(10 - posicao, 100 + posicao));
    expect(mudancasPorLoja([{ loja: 'Steam', pontos: muitos }])).toHaveLength(MAXIMO_DE_MUDANCAS_NA_LISTA);
  });

  it('frase sem gráfico: sem mudança, uma mudança ou nenhuma frase', () => {
    expect(fraseSemGrafico(HISTORICO)).toBe('Sem mudança de preço desde 01/11/2026');
    expect(fraseSemGrafico({ ...HISTORICO, melhorPreco: [ponto(20, 9000), ponto(4, 9900)] })).toBe(
      'Mudou uma vez: subiu para R$ 99,00 (+10%) em 17/11',
    );
    expect(fraseSemGrafico({ ...HISTORICO, melhorPreco: [ponto(20, 9000), ponto(4, 9900), ponto(2, 9000)] })).toBeNull();
    expect(fraseSemGrafico({ ...HISTORICO, observadoDesde: null })).toBeNull();
  });

  it('verificação de hoje, de outro dia e antiga', () => {
    expect(verificacao([FIM - 3_600_000, FIM - 5 * DIA_MS], FIM)).toEqual({ texto: 'Verificado hoje', antiga: false });
    expect(verificacao([FIM - DIA_MS], FIM)).toEqual({ texto: 'Verificado em 20/11/2026', antiga: false });
    expect(verificacao([FIM - 3 * DIA_MS], FIM)).toEqual({ texto: 'Verificado em 18/11/2026', antiga: true });
    expect(verificacao([], FIM)).toBeNull();
    expect(verificacao([Number.NaN], FIM)).toBeNull();
  });
});
