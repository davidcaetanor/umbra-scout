import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { ErroDaApi } from '../../../../core/erro';
import { TokenDaSessao } from '../../../../core/sessao';
import { HistoricoDoProduto } from '../../data/historico-do-produto';
import { MonitoramentoDoProdutoService } from '../../data/monitoramento-do-produto.service';
import { ProdutoDetalhado } from '../../data/produto-detalhado';
import { ProdutoService } from '../../data/produto.service';
import { DIA_MS } from './historico-exibido';
import { DetalhePage } from './detalhe.page';

const FIM = Date.parse('2026-11-21T12:00:00Z');

const PRODUTO: ProdutoDetalhado = {
  id: 42,
  nome: 'Hades II',
  tipo: 'JOGO',
  capa: null,
  categoria: null,
  melhorOferta: {
    loja: 'GOG',
    valorCentavos: 7240,
    preco: { atual: 'R$ 72,40', original: 'R$ 90,00', descontoPct: 20, loja: 'GOG' },
    gratuito: false,
    endereco: 'https://loja.exemplo/gog',
    coletadoEm: '2026-11-21T10:00:00Z',
    expiraEm: null,
    verificadoEm: Date.parse('2026-11-21T10:00:00Z'),
    situacao: 'vigente',
  },
  ofertas: [
    {
      loja: 'GOG',
      valorCentavos: 7240,
      preco: { atual: 'R$ 72,40', original: 'R$ 90,00', descontoPct: 20, loja: 'GOG' },
      gratuito: false,
      endereco: 'https://loja.exemplo/gog',
      coletadoEm: '2026-11-21T10:00:00Z',
      expiraEm: null,
      verificadoEm: Date.parse('2026-11-21T10:00:00Z'),
      situacao: 'vigente',
    },
    {
      loja: 'Steam',
      valorCentavos: 9000,
      preco: { atual: 'R$ 90,00', original: null, descontoPct: null, loja: 'Steam' },
      gratuito: false,
      endereco: 'https://loja.exemplo/steam',
      coletadoEm: '2026-11-20T10:00:00Z',
      expiraEm: null,
      verificadoEm: Date.parse('2026-11-21T10:00:00Z'),
      situacao: 'vigente',
    },
  ],
};

const HISTORICO: HistoricoDoProduto = {
  periodo: '90d',
  consultadoEm: FIM,
  observadoDesde: FIM - 40 * DIA_MS,
  melhorPreco: [{ instante: FIM - 40 * DIA_MS, valorCentavos: 7240 }],
  porLoja: [
    { loja: 'GOG', pontos: [{ instante: FIM - 40 * DIA_MS, valorCentavos: 7240 }] },
    { loja: 'Steam', pontos: [{ instante: FIM - 40 * DIA_MS, valorCentavos: 9000 }] },
    { loja: 'Nuuvem', pontos: [{ instante: FIM - 40 * DIA_MS, valorCentavos: 9500 }] },
  ],
  minimo: { valorCentavos: 7240, loja: 'GOG', em: FIM - 40 * DIA_MS },
};

const ponto = (dias: number, valorCentavos: number | null) => ({ instante: FIM - dias * DIA_MS, valorCentavos });

const MOVIMENTADO: HistoricoDoProduto = {
  ...HISTORICO,
  observadoDesde: FIM - 100 * DIA_MS,
  melhorPreco: [ponto(80, 9000), ponto(40, 7240), ponto(20, 8000), ponto(5, 7240)],
  porLoja: [
    { loja: 'GOG', pontos: [ponto(80, 9000), ponto(40, 7240)] },
    { loja: 'Steam', pontos: [ponto(80, 9000), ponto(20, 8000)] },
    { loja: 'Nuuvem', pontos: [ponto(80, 9500), ponto(5, 9000)] },
  ],
};

describe('DetalhePage', () => {
  let fixture: ComponentFixture<DetalhePage>;
  let detalhar: ReturnType<typeof vi.fn>;
  let historico: ReturnType<typeof vi.fn>;
  let buscar: ReturnType<typeof vi.fn>;
  const autenticado = signal(false);

  async function estabilizar(): Promise<void> {
    for (let volta = 0; volta < 3; volta++) {
      await new Promise((pronto) => setTimeout(pronto));
      await fixture.whenStable();
    }
  }

  async function abrir(id = '42'): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ProdutoService, useValue: { detalhar, historico } },
        { provide: MonitoramentoDoProdutoService, useValue: { buscar, criar: vi.fn() } },
        { provide: TokenDaSessao, useValue: { autenticado, vigente: () => (autenticado() ? 'token' : null) } },
      ],
    });
    fixture = TestBed.createComponent(DetalhePage);
    fixture.componentRef.setInput('id', id);
    await estabilizar();
    return fixture.nativeElement as HTMLElement;
  }

  const texto = (tela: HTMLElement) => tela.textContent?.replace(/\s+/g, ' ') ?? '';
  const botao = (tela: HTMLElement, nome: string) =>
    [...tela.querySelectorAll('button')].find((item) => item.textContent?.trim() === nome)!;

  beforeEach(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    detalhar = vi.fn().mockResolvedValue(PRODUTO);
    historico = vi.fn().mockResolvedValue(HISTORICO);
    buscar = vi.fn().mockResolvedValue(null);
    autenticado.set(false);
  });

  afterEach(() => vi.restoreAllMocks());

  it('mostra produto, melhor preço, tabela por loja e o título da aba', async () => {
    const tela = await abrir();

    expect(tela.querySelector('h1')?.textContent).toContain('Hades II');
    expect(texto(tela)).toContain('Ver na GOG');
    expect(texto(tela)).toContain('Mais barato');
    expect(texto(tela)).not.toContain('Verificado');
    expect(TestBed.inject(Title).getTitle()).toBe('Hades II · Umbra');
    expect(historico).toHaveBeenCalledWith(42, '90d');
  });

  it('id inválido mostra a página não encontrada sem chamar a API', async () => {
    const tela = await abrir('abc');

    expect(tela.querySelector('h1')?.textContent).toBe('Página não encontrada');
    expect(detalhar).not.toHaveBeenCalled();
    expect(TestBed.inject(Title).getTitle()).toBe('Página não encontrada · Umbra');
  });

  it('produto inexistente mostra a página não encontrada', async () => {
    detalhar.mockRejectedValue(new ErroDaApi({ tipo: 'nao-encontrado' }));

    const tela = await abrir();

    expect(tela.querySelector('h1')?.textContent).toBe('Página não encontrada');
  });

  it('falha do produto oferece tentar de novo', async () => {
    detalhar.mockRejectedValueOnce(new ErroDaApi({ tipo: 'rede' }));
    const tela = await abrir();

    botao(tela, 'Tentar de novo').click();
    await estabilizar();

    expect(detalhar).toHaveBeenCalledTimes(2);
    expect(tela.querySelector('h1')?.textContent).toContain('Hades II');
  });

  it('sem mudanças mostra a frase, o resumo sem a linha do gráfico e nenhum período', async () => {
    const tela = await abrir();

    expect(texto(tela)).toContain('Sem mudança de preço desde 12/10/2026');
    expect(tela.querySelector('canvas')).toBeNull();
    expect(texto(tela)).toMatch(/Menor registrado\s*R\$ 72,40\s*GOG, há 40 dias/);
    expect(texto(tela)).not.toContain('Melhor preço do dia');
    expect(tela.querySelector('[aria-label="Período"]')).toBeNull();
    expect(texto(tela)).not.toContain('Observado desde');
    expect(texto(tela)).not.toContain('Comparar lojas');
    expect(texto(tela)).not.toContain('disponível a partir de');
  });

  it('com uma mudança só descreve a mudança sem repetir na lista', async () => {
    historico.mockResolvedValue({
      ...HISTORICO,
      melhorPreco: [ponto(40, 9000), ponto(10, 7240)],
      porLoja: [{ loja: 'GOG', pontos: [ponto(40, 9000), ponto(10, 7240)] }],
    });

    const tela = await abrir();

    expect(texto(tela)).toContain('Mudou uma vez: caiu para R$ 72,40 (-20%) em 11/11');
    expect(tela.querySelector('canvas')).toBeNull();
    expect(tela.querySelector('[aria-label="Mudanças de preço"]')).toBeNull();
  });

  it('com duas mudanças mostra o gráfico, os períodos disponíveis e troca de período', async () => {
    historico.mockResolvedValue(MOVIMENTADO);
    const tela = await abrir();

    expect(tela.querySelector('canvas')).not.toBeNull();
    expect(texto(tela)).toContain('Observado desde');
    const lista = tela.querySelector('[aria-label="Mudanças de preço"]');
    expect(lista?.textContent?.replace(/\s+/g, ' ')).toContain('16/11: Nuuvem caiu para R$ 90,00 (-5%)');
    expect([...tela.querySelectorAll('[aria-label="Período"] button')].map((item) => item.textContent?.trim())).toEqual([
      '30 dias',
      '90 dias',
      '1 ano',
    ]);

    botao(tela, '30 dias').click();
    await estabilizar();
    expect(historico).toHaveBeenLastCalledWith(42, '30d');
  });

  it('com só 30 dias disponíveis esconde os períodos', async () => {
    historico.mockResolvedValue({ ...MOVIMENTADO, observadoDesde: FIM - 20 * DIA_MS });

    const tela = await abrir();

    expect(tela.querySelector('canvas')).not.toBeNull();
    expect(tela.querySelector('[aria-label="Período"]')).toBeNull();
  });

  it('comparar lojas mostra as que mudaram e limita a duas', async () => {
    historico.mockResolvedValue(MOVIMENTADO);
    const tela = await abrir();

    botao(tela, 'Comparar lojas').click();
    await estabilizar();
    const caixas = [...tela.querySelectorAll<HTMLInputElement>('input[type="checkbox"]')];

    expect(caixas.map((caixa) => [caixa.checked, caixa.disabled])).toEqual([
      [true, false],
      [true, false],
      [false, true],
    ]);
    caixas[0].click();
    await estabilizar();
    expect(caixas.map((caixa) => [caixa.checked, caixa.disabled])).toEqual([
      [false, false],
      [true, true],
      [false, false],
    ]);
    botao(tela, 'Ver o melhor preço').click();
    await estabilizar();
    expect(tela.querySelector('input[type="checkbox"]')).toBeNull();
  });

  it('com duas lojas que mudaram compara sem caixas de escolha', async () => {
    historico.mockResolvedValue({ ...MOVIMENTADO, porLoja: MOVIMENTADO.porLoja.slice(0, 2) });
    const tela = await abrir();

    botao(tela, 'Comparar lojas').click();
    await estabilizar();

    expect(tela.querySelector('input[type="checkbox"]')).toBeNull();
    expect(texto(tela)).toContain('a mais barata das duas');
  });

  it('sem histórico mostra o Umbra observando há N dias', async () => {
    historico.mockResolvedValue({ ...HISTORICO, melhorPreco: [], observadoDesde: FIM - DIA_MS });

    const tela = await abrir();

    expect(texto(tela)).toContain('O Umbra observa este produto há 1 dia.');
    expect(tela.querySelector('umbra-mascote')).not.toBeNull();
    expect(tela.querySelector('canvas')).toBeNull();
  });

  it('sem observação ainda diz que começou agora', async () => {
    historico.mockResolvedValue({ ...HISTORICO, melhorPreco: [], observadoDesde: null });

    const tela = await abrir();

    expect(texto(tela)).toContain('O Umbra começou a observar este produto agora.');
  });

  it('falha do histórico não derruba o produto', async () => {
    historico.mockRejectedValueOnce(new ErroDaApi({ tipo: 'rede' }));

    const tela = await abrir();

    expect(tela.querySelector('h1')?.textContent).toContain('Hades II');
    expect(tela.querySelector('[role="alert"]')).not.toBeNull();
    botao(tela, 'Tentar de novo').click();
    await estabilizar();
    expect(historico).toHaveBeenCalledTimes(2);
  });

  it('logado consulta o monitoramento e mostra o alvo na legenda e no bloco', async () => {
    autenticado.set(true);
    buscar.mockResolvedValue({ id: 9, precoAlvoCentavos: 6000 });

    const tela = await abrir();

    expect(buscar).toHaveBeenCalledWith(42);
    expect(texto(tela)).toContain('Você já monitora este produto com alvo de R$ 60,00');
    expect(texto(tela)).toMatch(/Alvo de busca\s*R\$ 60,00/);
  });

  it('deslogado não consulta monitoramento', async () => {
    const tela = await abrir();

    expect(buscar).not.toHaveBeenCalled();
    expect(tela.querySelector('#preco-alvo')).not.toBeNull();
  });

  it('hardware não mostra a coluna de desconto', async () => {
    detalhar.mockResolvedValue({ ...PRODUTO, tipo: 'HARDWARE', categoria: 'Placa de vídeo' });

    const tela = await abrir();

    expect([...tela.querySelectorAll('th')].map((th) => th.textContent?.trim())).not.toContain('Desconto');
    expect(texto(tela)).toContain('Placa de vídeo');
  });
});
