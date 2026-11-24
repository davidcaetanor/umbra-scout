import { OfertaDaLoja } from '../../data/produto-detalhado';
import { situacaoExibida } from './situacao-exibida';

const AGORA = Date.parse('2026-11-24T18:30:00Z');

function oferta(parcial: Partial<OfertaDaLoja>): OfertaDaLoja {
  return {
    loja: 'Epic',
    valorCentavos: 2395,
    preco: { atual: 'R$ 23,95', original: null, descontoPct: null, loja: 'Epic' },
    gratuito: false,
    endereco: 'https://loja.exemplo/epic',
    coletadoEm: '2026-11-24T10:00:00Z',
    expiraEm: null,
    verificadoEm: Date.parse('2026-11-24T18:05:00Z'),
    situacao: 'vigente',
    ...parcial,
  };
}

describe('situacaoExibida', () => {
  it('vigente verificada hoje mostra a hora, sem aviso', () => {
    expect(situacaoExibida(oferta({}), AGORA)).toEqual({ texto: 'Verificado hoje às 15:05', aviso: false });
  });

  it('vigente verificada em outro dia mostra o dia e o mês', () => {
    const verificadoEm = Date.parse('2026-11-23T18:05:00Z');
    expect(situacaoExibida(oferta({ verificadoEm }), AGORA).texto).toBe('Verificado em 23/11');
  });

  it('promoção no prazo mostra o fim e a última verificação, sem aviso', () => {
    const exibida = situacaoExibida(
      oferta({ situacao: 'promocao-no-prazo', expiraEm: Date.parse('2026-12-01T16:00:00Z'), verificadoEm: Date.parse('2026-11-19T12:00:00Z') }),
      AGORA,
    );
    expect(exibida).toEqual({ texto: 'Promoção até 01/12; última verificação em 19/11', aviso: false });
  });

  it('verificação atrasada avisa que pode estar desatualizado', () => {
    const exibida = situacaoExibida(oferta({ situacao: 'verificacao-atrasada', verificadoEm: Date.parse('2026-11-19T12:00:00Z') }), AGORA);
    expect(exibida).toEqual({ texto: 'Última verificação em 19/11, pode estar desatualizado', aviso: true });
  });

  it('promoção vencida sem coleta diz que aguarda nova verificação', () => {
    const exibida = situacaoExibida(
      oferta({ situacao: 'promocao-vencida-sem-coleta', expiraEm: Date.parse('2026-11-24T15:00:00Z'), verificadoEm: Date.parse('2026-11-24T12:00:00Z') }),
      AGORA,
    );
    expect(exibida).toEqual({ texto: 'Promoção terminou em 24/11, aguardando nova verificação', aviso: true });
  });
});
