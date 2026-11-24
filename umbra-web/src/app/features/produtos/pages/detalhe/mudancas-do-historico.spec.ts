import { HistoricoDoProduto, PontoDoHistorico } from '../../data/historico-do-produto';
import { DIA_MS } from './historico-exibido';
import {
  fraseSemGrafico,
  lojasQueMudaram,
  mostraGrafico,
  mudancas,
  mudancasPorLoja,
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

  it('deixa de fora variações abaixo de 5% e marca a mudança do menor preço', () => {
    const lista = mudancasPorLoja(
      [
        { loja: 'Steam', pontos: [ponto(10, 10000), ponto(8, 9700), ponto(6, 5000), ponto(4, 9000), ponto(2, 5000)] },
        { loja: 'GOG', pontos: [ponto(10, 6000), ponto(3, 5000)] },
      ],
      { valorCentavos: 5000, loja: 'Steam', em: FIM - 2 * DIA_MS },
    );

    expect(lista.map(({ data, texto, menor }) => `${data} ${texto}${menor ? ' *' : ''}`)).toEqual([
      '19/11 Steam caiu para R$ 50,00 (-44%) *',
      '18/11 GOG caiu para R$ 50,00 (-17%)',
      '17/11 Steam subiu para R$ 90,00 (+80%)',
      '15/11 Steam caiu para R$ 50,00 (-48%)',
    ]);
  });


  it('frase sem gráfico: sem mudança, uma mudança ou nenhuma frase', () => {
    expect(fraseSemGrafico(HISTORICO)).toBe('Sem mudança de preço desde 01/11/2026');
    expect(fraseSemGrafico({ ...HISTORICO, melhorPreco: [ponto(20, 9000), ponto(4, 9900)] })).toBe(
      'Mudou uma vez: subiu para R$ 99,00 (+10%) em 17/11',
    );
    expect(fraseSemGrafico({ ...HISTORICO, melhorPreco: [ponto(20, 9000), ponto(4, 9900), ponto(2, 9000)] })).toBeNull();
    expect(fraseSemGrafico({ ...HISTORICO, observadoDesde: null })).toBeNull();
  });
});
