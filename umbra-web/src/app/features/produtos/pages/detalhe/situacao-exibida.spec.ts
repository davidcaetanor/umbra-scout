import { OfertaDaLoja } from '../../data/produto-detalhado';
import { situacaoExibida } from './situacao-exibida';

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
  it('vigente não mostra nada', () => {
    expect(situacaoExibida(oferta({}))).toBeNull();
  });

  it('promoção no prazo mostra só o fim da promoção', () => {
    const exibida = situacaoExibida(oferta({ situacao: 'promocao-no-prazo', expiraEm: Date.parse('2026-12-01T16:00:00Z') }));
    expect(exibida).toEqual({ texto: 'Promoção até 01/12', aviso: false });
  });

  it('verificação atrasada avisa que pode estar desatualizado', () => {
    const exibida = situacaoExibida(oferta({ situacao: 'verificacao-atrasada', verificadoEm: Date.parse('2026-11-19T12:00:00Z') }));
    expect(exibida).toEqual({ texto: 'Última verificação em 19/11, pode estar desatualizado', aviso: true });
  });

  it('promoção vencida sem coleta diz que aguarda nova verificação', () => {
    const exibida = situacaoExibida(
      oferta({ situacao: 'promocao-vencida-sem-coleta', expiraEm: Date.parse('2026-11-24T15:00:00Z'), verificadoEm: Date.parse('2026-11-24T12:00:00Z') }),
    );
    expect(exibida).toEqual({ texto: 'Promoção terminou em 24/11, aguardando nova verificação', aviso: true });
  });
});
