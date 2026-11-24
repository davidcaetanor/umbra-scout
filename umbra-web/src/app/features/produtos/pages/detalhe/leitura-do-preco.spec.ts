import { HistoricoDoProduto } from '../../data/historico-do-produto';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import { DIA_MS } from './historico-exibido';
import { leituraDoPreco } from './leitura-do-preco';

const AGORA = Date.parse('2026-11-21T12:00:00Z');

const OFERTA: OfertaDaLoja = {
  loja: 'GOG',
  valorCentavos: 1000,
  preco: { atual: 'R$ 10,00', original: null, descontoPct: null, loja: 'GOG' },
  gratuito: false,
  endereco: 'https://loja.exemplo',
  coletadoEm: '2026-11-21T10:00:00Z',
  expiraEm: null,
  verificadoEm: Date.parse('2026-11-21T10:00:00Z'),
  situacao: 'vigente',
};

const HISTORICO: HistoricoDoProduto = {
  periodo: '30d',
  consultadoEm: AGORA,
  observadoDesde: AGORA - 40 * DIA_MS,
  melhorPreco: [1000, 2000, 3000, null, 4000, 5000].map((valorCentavos, posicao) => ({
    instante: AGORA - (30 - posicao) * DIA_MS,
    valorCentavos,
  })),
  porLoja: [],
  minimo: { valorCentavos: 1000, loja: 'GOG', em: AGORA - 30 * DIA_MS },
};

const comPreco = (valorCentavos: number, parcial: Partial<OfertaDaLoja> = {}) => ({ ...OFERTA, valorCentavos, ...parcial });

describe('selo do preço', () => {
  it('bom momento no mínimo e até 10% acima dele', () => {
    expect(leituraDoPreco(OFERTA, HISTORICO)).toEqual({ titulo: 'Bom momento', detalhe: 'No menor preço já registrado', tom: 'bom' });
    expect(leituraDoPreco(comPreco(1100), HISTORICO)).toEqual({
      titulo: 'Bom momento',
      detalhe: 'Perto do menor preço já registrado',
      tom: 'bom',
    });
  });

  it('preço de costume até a mediana do período, ignorando lacunas', () => {
    expect(leituraDoPreco(comPreco(1101), HISTORICO)?.tom).toBe('costume');
    expect(leituraDoPreco(comPreco(3000), HISTORICO)).toEqual({
      titulo: 'Preço de costume',
      detalhe: 'Dentro do comum do período',
      tom: 'costume',
    });
  });

  it('já esteve mais barato acima da mediana, com mediana de quantidade par', () => {
    const par = { ...HISTORICO, melhorPreco: HISTORICO.melhorPreco.slice(0, 5) };
    expect(leituraDoPreco(comPreco(2600), par)?.tom).toBe('mais-barato');
    expect(leituraDoPreco(comPreco(2500), par)?.tom).toBe('costume');
    expect(leituraDoPreco(comPreco(3001), HISTORICO)).toEqual({ titulo: 'Já esteve mais barato', detalhe: 'Vale monitorar', tom: 'mais-barato' });
  });

  it('com menos de três pontos diz que há pouco histórico, mantendo o prazo', () => {
    const doisPontos = { ...HISTORICO, melhorPreco: HISTORICO.melhorPreco.slice(0, 2) };
    expect(leituraDoPreco(comPreco(1000), doisPontos)).toEqual({ titulo: 'Pouco histórico ainda', detalhe: null, tom: 'pouco' });
    expect(leituraDoPreco(comPreco(1000), { ...HISTORICO, melhorPreco: [] })?.tom).toBe('pouco');

    const comPrazo = leituraDoPreco(comPreco(1000, { situacao: 'promocao-no-prazo', expiraEm: Date.parse('2026-11-24T23:00:00-03:00') }), doisPontos);
    expect(comPrazo?.detalhe).toBe('Promoção da GOG termina em 24/11');
  });

  it('acrescenta o prazo da promoção pela data de São Paulo', () => {
    const emData = leituraDoPreco(comPreco(1000, { situacao: 'promocao-no-prazo', expiraEm: Date.parse('2026-11-24T23:00:00-03:00') }), HISTORICO);
    expect(emData?.detalhe).toBe('No menor preço já registrado, promoção da GOG termina em 24/11');

    const amanha = leituraDoPreco(comPreco(1000, { situacao: 'promocao-no-prazo', expiraEm: Date.parse('2026-11-22T23:59:00-03:00') }), HISTORICO);
    expect(amanha?.detalhe).toContain('termina amanhã');

    const hoje = leituraDoPreco(comPreco(3001, { situacao: 'promocao-no-prazo', expiraEm: Date.parse('2026-11-21T22:00:00-03:00') }), HISTORICO);
    expect(hoje?.detalhe).toBe('Vale monitorar, promoção da GOG termina hoje');

    expect(leituraDoPreco(comPreco(1000, { expiraEm: AGORA - 1 }), HISTORICO)?.detalhe).toBe('No menor preço já registrado');
  });

  it('usa a situação da oferta vinda da API, sem decidir o prazo no navegador', () => {
    const vencidaPelaApi = comPreco(1000, { situacao: 'vigente', expiraEm: AGORA + DIA_MS });
    expect(leituraDoPreco(vencidaPelaApi, HISTORICO)?.detalhe).toBe('No menor preço já registrado');

    const atrasada = comPreco(1000, { situacao: 'verificacao-atrasada', verificadoEm: Date.parse('2026-11-18T12:00:00Z') });
    expect(leituraDoPreco(atrasada, HISTORICO)?.detalhe).toBe('No menor preço já registrado, preço verificado pela última vez em 18/11');
  });

  it('mínimo grátis só é bom momento se hoje também for grátis', () => {
    const gratis = { ...HISTORICO, minimo: { ...HISTORICO.minimo!, valorCentavos: 0 } };
    expect(leituraDoPreco(comPreco(0), gratis)?.tom).toBe('bom');
    expect(leituraDoPreco(comPreco(500), gratis)?.tom).toBe('costume');
  });

  it('fica sem selo sem oferta hoje ou sem mínimo', () => {
    expect(leituraDoPreco(null, HISTORICO)).toBeNull();
    expect(leituraDoPreco(OFERTA, { ...HISTORICO, minimo: null })).toBeNull();
  });
});
