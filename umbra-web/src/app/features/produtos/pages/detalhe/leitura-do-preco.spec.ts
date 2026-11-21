import { MinimoDoHistorico } from '../../data/historico-do-produto';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import { DIA_MS } from './historico-exibido';
import { leituraDoPreco } from './leitura-do-preco';

const AGORA = Date.parse('2026-11-21T12:00:00Z');

const OFERTA: OfertaDaLoja = {
  loja: 'GOG',
  valorCentavos: 2399,
  preco: { atual: 'R$ 23,99', original: null, descontoPct: null, loja: 'GOG' },
  gratuito: false,
  endereco: 'https://loja.exemplo',
  coletadoEm: '2026-11-21T10:00:00Z',
  expiraEm: null,
};

const MINIMO: MinimoDoHistorico = { valorCentavos: 2399, loja: 'GOG', em: AGORA - 5 * DIA_MS };

describe('leitura do preço', () => {
  it('diz que está no mínimo, sem detalhe quando não há prazo', () => {
    expect(leituraDoPreco(OFERTA, MINIMO, AGORA)).toEqual({ titulo: 'No menor preço já registrado', detalhe: null, noMinimo: true });
  });

  it('mostra quanto está acima do mínimo, em reais e em porcentagem', () => {
    const leitura = leituraDoPreco({ ...OFERTA, valorCentavos: 2799 }, MINIMO, AGORA);

    expect(leitura).toEqual({ titulo: 'R$ 4,00 acima do menor registrado', detalhe: '+17%', noMinimo: false });
  });

  it('acrescenta o prazo da promoção quando a loja informa', () => {
    const depois = leituraDoPreco({ ...OFERTA, expiraEm: Date.parse('2026-11-24T23:00:00-03:00') }, MINIMO, AGORA);
    expect(depois?.detalhe).toBe('Promoção da GOG termina em 24/11');

    const amanha = leituraDoPreco({ ...OFERTA, expiraEm: Date.parse('2026-11-22T23:59:00-03:00') }, MINIMO, AGORA);
    expect(amanha?.detalhe).toBe('Promoção da GOG termina amanhã');

    const hoje = leituraDoPreco({ ...OFERTA, valorCentavos: 2799, expiraEm: Date.parse('2026-11-21T22:00:00-03:00') }, MINIMO, AGORA);
    expect(hoje?.detalhe).toBe('+17%, promoção da GOG termina hoje');
  });

  it('ignora prazo vencido e porcentagem sobre mínimo grátis', () => {
    const leitura = leituraDoPreco({ ...OFERTA, valorCentavos: 500, expiraEm: AGORA - 1 }, { ...MINIMO, valorCentavos: 0 }, AGORA);

    expect(leitura).toEqual({ titulo: 'R$ 5,00 acima do menor registrado', detalhe: null, noMinimo: false });
  });

  it('fica sem leitura sem oferta hoje ou sem mínimo', () => {
    expect(leituraDoPreco(null, MINIMO, AGORA)).toBeNull();
    expect(leituraDoPreco(OFERTA, null, AGORA)).toBeNull();
  });
});
