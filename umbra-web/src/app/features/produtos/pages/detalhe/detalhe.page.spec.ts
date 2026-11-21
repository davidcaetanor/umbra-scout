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
  },
  ofertas: [
    {
      loja: 'GOG',
      valorCentavos: 7240,
      preco: { atual: 'R$ 72,40', original: 'R$ 90,00', descontoPct: 20, loja: 'GOG' },
      gratuito: false,
      endereco: 'https://loja.exemplo/gog',
      coletadoEm: '2026-11-21T10:00:00Z',
    },
    {
      loja: 'Steam',
      valorCentavos: 9000,
      preco: { atual: 'R$ 90,00', original: null, descontoPct: null, loja: 'Steam' },
      gratuito: false,
      endereco: 'https://loja.exemplo/steam',
      coletadoEm: '2026-11-20T10:00:00Z',
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
    expect(texto(tela)).toContain('Atualizado em 21/11/2026 às 07:00');
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

  it('desliga o período sem histórico suficiente e explica', async () => {
    const tela = await abrir();

    expect(botao(tela, '1 ano').disabled).toBe(true);
    expect(botao(tela, '90 dias').getAttribute('aria-pressed')).toBe('true');
    expect(texto(tela)).toContain('1 ano disponível a partir de 91 dias observados');

    botao(tela, '30 dias').click();
    await estabilizar();
    expect(historico).toHaveBeenLastCalledWith(42, '30d');
  });

  it('por loja limita a escolha a duas lojas', async () => {
    const tela = await abrir();

    botao(tela, 'Por loja').click();
    await estabilizar();
    const caixas = [...tela.querySelectorAll<HTMLInputElement>('input[type="checkbox"]')];

    expect(caixas.map((caixa) => [caixa.checked, caixa.disabled])).toEqual([
      [true, false],
      [true, false],
      [false, true],
    ]);
    caixas[0].click();
    await estabilizar();
    expect(caixas[2].disabled).toBe(false);
    expect(caixas.map((caixa) => [caixa.checked, caixa.disabled])).toEqual([
      [false, false],
      [true, true],
      [false, false],
    ]);
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
    expect(texto(tela)).toContain('você entra na sua conta e volta para cá');
  });

  it('hardware não mostra a coluna de desconto', async () => {
    detalhar.mockResolvedValue({ ...PRODUTO, tipo: 'HARDWARE', categoria: 'Placa de vídeo' });

    const tela = await abrir();

    expect([...tela.querySelectorAll('th')].map((th) => th.textContent?.trim())).not.toContain('Desconto');
    expect(texto(tela)).toContain('Placa de vídeo');
  });
});
