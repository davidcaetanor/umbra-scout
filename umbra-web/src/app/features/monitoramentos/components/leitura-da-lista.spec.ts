import { MonitoramentoExibido, SituacaoExibida } from './monitoramento-exibido';
import { FRASES_DA_RONDA, MAXIMO_DO_NOME_NA_FALA, alvoAConfirmar, atingidosPrimeiro, avisoDoPreco, chaveDaNovidade, contagem, distancia, fraseDaRonda, nomeCurto, preenchimento, temNovidade, todosPausados } from './leitura-da-lista';

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
    avisadoEm: null,
    foraDeVenda: false,
    ...parcial,
  };
}

describe('leitura da lista de monitoramentos', () => {
  it('so ha novidade com alvo atingido e aviso registrado pela API', () => {
    const avisadoEm = Date.parse('2026-11-27T18:30:00Z');

    expect(temNovidade(item({ alvoAtingido: true, avisadoEm }))).toBe(true);
    expect(temNovidade(item({ alvoAtingido: true, avisadoEm: null }))).toBe(false);
    expect(temNovidade(item({ alvoAtingido: false, avisadoEm }))).toBe(false);
  });

  it('a chave da novidade muda com um aviso novo e nao com o preco', () => {
    const primeiro = item({ alvoAtingido: true, avisadoEm: Date.parse('2026-11-27T18:30:00Z') });
    const outroPreco = { ...primeiro, distanciaDoAlvoCentavos: -1 };
    const avisoNovo = { ...primeiro, avisadoEm: Date.parse('2026-11-28T18:30:00Z') };

    expect(chaveDaNovidade(outroPreco)).toBe(chaveDaNovidade(primeiro));
    expect(chaveDaNovidade(avisoNovo)).not.toBe(chaveDaNovidade(primeiro));
  });

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

  it('preco abaixo do alvo com verificacao atrasada fica a confirmar, nunca atingido', () => {
    const preco = { ...item().melhorPreco!, valorCentavos: 14000 };
    const abaixo = (situacao: SituacaoExibida, parcial: Partial<MonitoramentoExibido> = {}) =>
      alvoAConfirmar(item({ melhorPreco: { ...preco, situacao }, distanciaDoAlvoCentavos: -1000, ...parcial }));

    expect(abaixo('verificacao-atrasada')).toBe(true);
    expect(abaixo('promocao-vencida-sem-coleta')).toBe(true);
    expect(abaixo('vigente')).toBe(false);
    expect(abaixo('verificacao-atrasada', { alvoAtingido: true })).toBe(false);
    expect(abaixo('verificacao-atrasada', { ativo: false })).toBe(false);
    expect(alvoAConfirmar(item({ melhorPreco: { ...item().melhorPreco!, situacao: 'verificacao-atrasada' } }))).toBe(false);
    expect(alvoAConfirmar(item({ melhorPreco: null }))).toBe(false);
  });

  it('nome curto respeita o teto, corta no espaco e fecha com reticencias', () => {
    const placa = 'Placa de Vídeo ASUS PRIME RTX 5070 OC 12G WHITE NVIDIA Geforce, 12GB, GDDR7, OpenGL 4.6';
    const curto = nomeCurto(placa);

    expect(curto).toBe('Placa de Vídeo ASUS PRIME RTX 5070 OC 12G WHITE NVIDIA…');
    expect(curto.length).toBeLessThanOrEqual(MAXIMO_DO_NOME_NA_FALA + 1);
    expect(nomeCurto('Hollow Knight: Silksong')).toBe('Hollow Knight: Silksong');
    expect(nomeCurto('HD Interno Seagate Exos 7E10, NAS, 8TB', 30)).toBe('HD Interno Seagate Exos 7E10…');
    expect(nomeCurto('Supercalifragilisticexpialidocious', 10)).toBe('Supercalif…');
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
