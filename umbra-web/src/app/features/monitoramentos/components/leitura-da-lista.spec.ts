import { MonitoramentoExibido } from './monitoramento-exibido';
import { FRASES_DA_RONDA, atingidosPrimeiro, avisoDoPreco, contagem, distancia, fraseDaRonda, preenchimento, todosPausados } from './leitura-da-lista';

function item(parcial: Partial<MonitoramentoExibido> = {}): MonitoramentoExibido {
  return {
    id: 1,
    produtoId: 10,
    nome: 'Black Myth: Wukong',
    capa: null,
    tipo: 'JOGO',
    precoAlvoCentavos: 15000,
    ativo: true,
    melhorPreco: {
      valorCentavos: 17249,
      loja: 'GreenManGaming',
      gratuito: false,
      situacao: 'vigente',
      expiraEm: null,
      verificadoEm: Date.parse('2026-11-24T10:00:00Z'),
    },
    distanciaDoAlvoCentavos: 2249,
    alvoAtingido: false,
    foraDeVenda: false,
    ...parcial,
  };
}

describe('leitura da lista de monitoramentos', () => {
  it('conta ativos e pausados no singular e no plural', () => {
    expect(contagem([item(), item(), item(), item({ ativo: false })])).toBe('3 ativos · 1 pausado');
    expect(contagem([item({ ativo: false }), item({ ativo: false })])).toBe('2 pausados');
    expect(contagem([item()])).toBe('1 ativo');
  });

  it('todos pausados exige lista com itens e nenhum ativo', () => {
    expect(todosPausados([])).toBe(false);
    expect(todosPausados([item({ ativo: false })])).toBe(true);
    expect(todosPausados([item({ ativo: false }), item()])).toBe(false);
  });

  it('barra mede alvo sobre preco atual, cheia quando atingido e vazia sem preco', () => {
    expect(preenchimento(item())).toBe(87);
    expect(preenchimento(item({ alvoAtingido: true }))).toBe(100);
    expect(preenchimento(item({ melhorPreco: null }))).toBe(0);
  });

  it('distancia diz quanto falta ou quanto passou do alvo', () => {
    expect(distancia(item())).toBe('faltam R$ 22,49');
    expect(distancia(item({ distanciaDoAlvoCentavos: -1010 }))).toBe('R$ 10,10 abaixo do alvo');
    expect(distancia(item({ distanciaDoAlvoCentavos: 0 }))).toBe('no alvo');
    expect(distancia(item({ distanciaDoAlvoCentavos: null }))).toBeNull();
  });

  it('so o prazo da promocao vira aviso; verificacao atrasada fica em silencio', () => {
    const preco = item().melhorPreco!;
    const expiraEm = Date.parse('2026-12-01T12:00:00Z');
    expect(avisoDoPreco(item())).toBeNull();
    expect(avisoDoPreco(item({ melhorPreco: { ...preco, situacao: 'verificacao-atrasada' } }))).toBeNull();
    expect(avisoDoPreco(item({ melhorPreco: { ...preco, situacao: 'promocao-no-prazo', expiraEm } }))).toBe('Promoção até 01/12');
    expect(avisoDoPreco(item({ melhorPreco: { ...preco, situacao: 'promocao-vencida-sem-coleta', expiraEm } }))).toBe(
      'Promoção terminou em 01/12',
    );
  });

  it('alvos atingidos sobem para o topo e o resto mantem a ordem', () => {
    const lista = [item({ id: 1 }), item({ id: 2, alvoAtingido: true }), item({ id: 3 }), item({ id: 4, alvoAtingido: true })];

    expect(atingidosPrimeiro(lista).map((atual) => atual.id)).toEqual([2, 4, 1, 3]);
  });

  it('frase da ronda muda de um dia para o outro e se repete no mesmo dia', () => {
    const manha = Date.parse('2026-11-24T10:00:00-03:00');
    const noite = Date.parse('2026-11-24T23:30:00-03:00');
    const amanha = Date.parse('2026-11-25T08:00:00-03:00');

    expect(fraseDaRonda(manha)).toBe(fraseDaRonda(noite));
    expect(fraseDaRonda(amanha)).not.toBe(fraseDaRonda(manha));
    expect(FRASES_DA_RONDA).toContain(fraseDaRonda(manha));
  });
});
