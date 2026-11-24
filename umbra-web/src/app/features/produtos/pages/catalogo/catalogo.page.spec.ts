import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Observable, Subject } from 'rxjs';
import { ErroDaApi } from '../../../../core/erro';
import { CatalogoService } from '../../data/catalogo.service';
import { FiltroDaBusca } from '../../data/filtro-da-busca';
import { ProdutoDoCatalogo } from '../../data/produto-do-catalogo';
import { ProdutoExterno } from '../../data/produto-externo';
import { ResultadoDaBusca } from '../../data/resultado-da-busca';
import { CatalogoPage } from './catalogo.page';
import { FILTRO_INICIAL } from './filtro-da-url';

const PRODUTO: ProdutoDoCatalogo = {
  id: 1,
  nome: 'Hollow Knight',
  tipo: 'JOGO',
  capa: null,
  categoria: null,
  preco: { atual: 'R$ 23,99', original: 'R$ 47,99', descontoPct: 50, loja: 'Steam' },
};

const EXTERNO: ProdutoExterno = {
  gid: 'g1',
  nome: 'Hollow Edition',
  capa: null,
  preco: { atual: 'R$ 35,99', original: null, descontoPct: null, loja: 'GOG' },
  enderecoDaLoja: 'https://loja.exemplo/hollow',
};

function resultado(produtos: ProdutoDoCatalogo[], pagina = 0, totalDePaginas = 1, total = produtos.length): ResultadoDaBusca {
  return { produtos, pagina, totalDePaginas, total };
}

class CatalogoFalso {
  readonly buscas: FiltroDaBusca[] = [];
  readonly externas: FiltroDaBusca[] = [];
  respostaLocal = new Subject<ResultadoDaBusca>();
  respostaExterna = new Subject<readonly ProdutoExterno[]>();

  buscar(filtro: FiltroDaBusca): Observable<ResultadoDaBusca> {
    this.buscas.push(filtro);
    this.respostaLocal = new Subject<ResultadoDaBusca>();
    return this.respostaLocal;
  }

  buscarForaDoCatalogo(filtro: FiltroDaBusca): Observable<readonly ProdutoExterno[]> {
    this.externas.push(filtro);
    this.respostaExterna = new Subject<readonly ProdutoExterno[]>();
    return this.respostaExterna;
  }
}

describe('CatalogoPage', () => {
  let catalogo: CatalogoFalso;
  let harness: RouterTestingHarness;
  let tela: HTMLElement;

  async function abrir(url = '/catalogo'): Promise<void> {
    catalogo = new CatalogoFalso();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'catalogo', component: CatalogoPage }]),
        { provide: CatalogoService, useValue: catalogo },
      ],
    });
    harness = await RouterTestingHarness.create(url);
    tela = harness.routeNativeElement!;
  }

  async function responder(dados: ResultadoDaBusca): Promise<void> {
    catalogo.respostaLocal.next(dados);
    catalogo.respostaLocal.complete();
    await harness.fixture.whenStable();
  }

  async function digitar(texto: string): Promise<void> {
    const campo = tela.querySelector<HTMLInputElement>('#busca')!;
    campo.value = texto;
    campo.dispatchEvent(new Event('input'));
    harness.fixture.detectChanges();
  }

  const umbra = () => [...tela.querySelectorAll('umbra-mascote')].map((m) => m.querySelector('.sprite')?.getAttribute('style') ?? '');
  const url = () => TestBed.inject(Router).url;

  afterEach(() => vi.useRealTimers());

  function escolher(seletor: string, valor: string): void {
    const campo = tela.querySelector<HTMLInputElement | HTMLSelectElement>(seletor)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('change'));
  }

  it('ordenacoes de oferta e desconto minimo so aparecem para jogos', async () => {
    await abrir('/catalogo?tipo=HARDWARE');
    const opcoes = () => [...tela.querySelectorAll('#ordenacao option')].map((opcao) => opcao.getAttribute('value'));

    expect(opcoes()).toEqual(['nome', 'menorPreco', 'melhoresOfertas']);
    expect(tela.querySelector('#desconto')).toBeNull();
    expect([...tela.querySelectorAll('#loja option')].map((o) => o.getAttribute('value'))).toEqual(['', 'KABUM']);

    await harness.navigateByUrl('/catalogo?tipo=JOGO');
    expect(opcoes()).toEqual(['nome', 'menorPreco', 'melhoresOfertas', 'maiorDesconto']);
    expect(tela.querySelector('#desconto')).not.toBeNull();
  });

  it('faixa de preco, desconto e loja vao para a URL e voltam para a primeira pagina', async () => {
    await abrir('/catalogo?tipo=JOGO&pagina=3');

    escolher('#preco-min', '10');
    await harness.fixture.whenStable();
    escolher('#preco-max', '200,50');
    await harness.fixture.whenStable();
    escolher('#desconto', '50');
    await harness.fixture.whenStable();
    escolher('#loja', 'STEAM');
    await harness.fixture.whenStable();

    expect(url()).toBe('/catalogo?tipo=JOGO&precoMin=10&precoMax=200.5&descontoMin=50&loja=STEAM');
    expect(catalogo.buscas.at(-1)).toMatchObject({ precoMin: 1000, precoMax: 20050, descontoMin: 50, loja: 'STEAM', pagina: 0 });
    expect(tela.querySelector<HTMLInputElement>('#preco-max')?.value).toBe('200,50');
  });

  it('faixa invertida avisa no campo e nao busca', async () => {
    await abrir('/catalogo?precoMin=300');
    const buscasAntes = catalogo.buscas.length;

    escolher('#preco-max', '100');
    await harness.fixture.whenStable();

    expect(tela.querySelector('#preco-dica')?.textContent).toBe('O valor de "até" não pode ser menor que o de "de".');
    expect(tela.querySelector('#preco-dica')?.classList).toContain('text-erro');
    expect(tela.querySelector('#preco-max')?.getAttribute('aria-invalid')).toBe('true');
    expect(catalogo.buscas.length).toBe(buscasAntes);
    expect(url()).toBe('/catalogo?precoMin=300');
  });

  it.each([
    ['abc', 'Use só números, como 49,90.'],
    ['-5', 'Use só números, como 49,90.'],
    ['12,5,0', 'Use só números, como 49,90.'],
    ['100.000,01', 'O preço vai até R$ 100.000,00.'],
  ])('valor %s no preco avisa, mantem o filtro e nao busca', async (valor, aviso) => {
    await abrir('/catalogo?precoMin=30');
    const buscasAntes = catalogo.buscas.length;

    escolher('#preco-min', valor);
    await harness.fixture.whenStable();

    expect(tela.querySelector('#preco-dica')?.textContent).toBe(aviso);
    expect(tela.querySelector('#preco-min')?.getAttribute('aria-invalid')).toBe('true');
    expect(catalogo.buscas.length).toBe(buscasAntes);
    expect(url()).toBe('/catalogo?precoMin=30');
  });

  it('corrigir o valor tira o aviso e aplica', async () => {
    await abrir('/catalogo');
    escolher('#preco-min', 'abc');
    await harness.fixture.whenStable();

    escolher('#preco-min', '1.500,00');
    await harness.fixture.whenStable();

    expect(tela.querySelector('#preco-dica')).toBeNull();
    expect(url()).toBe('/catalogo?precoMin=1500');
  });

  it('URL com faixa invertida avisa e fica so com o De', async () => {
    await abrir('/catalogo?precoMin=300&precoMax=100');
    await harness.fixture.whenStable();

    expect(url()).toBe('/catalogo?precoMin=300');
    expect(tela.querySelector('#preco-dica')?.textContent).toBe('O valor de "até" não pode ser menor que o de "de".');
  });

  it('URL com parametros ignorados volta limpa', async () => {
    await abrir('/catalogo?tipo=JOGO&categoria=GPU&descontoMin=33&pagina=abc&loja=TERABYTE');
    await harness.fixture.whenStable();

    expect(url()).toBe('/catalogo?tipo=JOGO');
  });

  it('trocar de jogo para hardware tira o criterio de oferta, o desconto e a loja de jogo', async () => {
    await abrir('/catalogo?tipo=JOGO&ordenacao=melhoresOfertas&descontoMin=50&loja=GOG');

    [...tela.querySelectorAll<HTMLButtonElement>('button.chip')].find((botao) => botao.textContent?.includes('Hardware'))!.click();
    await harness.fixture.whenStable();

    expect(url()).toBe('/catalogo?tipo=HARDWARE');
  });

  it('no celular os filtros ficam recolhidos e o botao conta os ativos', async () => {
    await abrir('/catalogo?tipo=JOGO&precoMin=10&loja=GOG');
    const botao = tela.querySelector<HTMLButtonElement>('button[aria-controls="filtros-da-busca"]')!;

    expect(botao.textContent?.trim()).toBe('Filtros (2)');
    expect(botao.getAttribute('aria-expanded')).toBe('false');

    botao.click();
    await harness.fixture.whenStable();

    expect(botao.getAttribute('aria-expanded')).toBe('true');
    expect(tela.querySelector('#filtros-da-busca')?.classList).toContain('grid');
  });

  it('mostra o esqueleto, depois os cards e anuncia a contagem', async () => {
    await abrir();

    expect(tela.querySelector('[role="status"] .sr-only')?.textContent).toContain('Carregando produtos');

    await responder(resultado([PRODUTO]));

    expect(tela.querySelector('umbra-card-produto h3')?.textContent?.trim()).toBe('Hollow Knight');
    expect(tela.textContent).toContain('1 produto encontrado');
    expect(umbra()[0]).toContain('observando');
  });

  it('abre o catalogo em melhores ofertas, com tudo, e a busca passa para nome', async () => {
    await abrir('/catalogo');

    expect(catalogo.buscas[0]).toEqual(FILTRO_INICIAL);
    expect(catalogo.buscas[0].ordenacao).toBe('melhoresOfertas');
    expect(tela.querySelector<HTMLSelectElement>('#ordenacao')?.value).toBe('melhoresOfertas');
  });

  it('le os filtros da URL e ignora os invalidos', async () => {
    await abrir('/catalogo?nome=hollow&tipo=FILME&pagina=2');

    expect(catalogo.buscas[0]).toEqual({ ...FILTRO_INICIAL, termo: 'hollow', ordenacao: 'nome', pagina: 1 });
  });

  it('busca uma vez so depois da pausa na digitacao e troca a URL sem empilhar historico', async () => {
    await abrir();
    await responder(resultado([PRODUTO]));
    vi.useFakeTimers();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate');

    await digitar('hol');
    await vi.advanceTimersByTimeAsync(100);
    await digitar('hollow');
    await vi.advanceTimersByTimeAsync(300);

    expect(navegar).toHaveBeenCalledOnce();
    expect(navegar.mock.calls[0][1]).toMatchObject({ queryParams: { nome: 'hollow' }, replaceUrl: true });
  });

  it('termo curto nao busca e explica o minimo', async () => {
    await abrir();
    await responder(resultado([PRODUTO]));
    vi.useFakeTimers();

    await digitar('ho');
    await vi.advanceTimersByTimeAsync(300);
    harness.fixture.detectChanges();

    expect(catalogo.buscas).toHaveLength(1);
    expect(tela.querySelector('#busca-dica')?.textContent).toContain('ao menos 3 letras');
    expect(tela.querySelector('#busca')?.getAttribute('aria-describedby')).toBe('busca-dica');
  });

  it('busca local vazia procura fora do catalogo sozinha, com o Umbra buscando', async () => {
    await abrir('/catalogo?nome=hollow');
    await responder(resultado([]));

    expect(catalogo.externas).toHaveLength(1);
    expect(umbra()).toHaveLength(1);
    expect(umbra()[0]).toContain('buscando');

    catalogo.respostaExterna.next([EXTERNO]);
    catalogo.respostaExterna.complete();
    await harness.fixture.whenStable();

    expect(tela.querySelector('umbra-card-externo h3')?.textContent?.trim()).toBe('Hollow Edition');
    expect(tela.querySelector('umbra-card-externo a')?.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('nada no catalogo nem fora mostra so o Umbra perdido e oferece limpar os filtros', async () => {
    await abrir('/catalogo?nome=xyzw&tipo=JOGO');
    await responder(resultado([]));
    catalogo.respostaExterna.next([]);
    catalogo.respostaExterna.complete();
    await harness.fixture.whenStable();

    expect(tela.textContent).toContain('Nada com "xyzw"');
    expect(umbra().every((estilo) => estilo.includes('perdido'))).toBe(true);

    tela.querySelector<HTMLButtonElement>('[vazio] button')!.click();
    await harness.fixture.whenStable();

    expect(url()).toBe('/catalogo');
  });

  it('com resultado local a busca externa so sai pelo botao', async () => {
    await abrir('/catalogo?nome=hollow');
    await responder(resultado([PRODUTO]));

    expect(catalogo.externas).toHaveLength(0);

    [...tela.querySelectorAll('button')].find((b) => b.textContent?.includes('fora do catálogo'))!.click();
    await harness.fixture.whenStable();

    expect(catalogo.externas).toHaveLength(1);
  });

  it('erro mostra o bloco com o Umbra em erro, sem o do cabecalho, e tentar de novo busca outra vez', async () => {
    await abrir();
    catalogo.respostaLocal.error(new ErroDaApi({ tipo: 'rede' }));
    await harness.fixture.whenStable();

    expect(tela.querySelector('[role="alert"]')?.textContent).toContain('Não conseguimos falar com o Umbra agora.');
    expect(umbra()).toHaveLength(1);
    expect(umbra()[0]).toContain('erro');

    tela.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    await harness.fixture.whenStable();

    expect(catalogo.buscas).toHaveLength(2);
  });

  it('pagina alem do fim vai para a ultima existente', async () => {
    await abrir('/catalogo?pagina=9');
    await responder(resultado([PRODUTO], 8, 3, 41));

    expect(url()).toBe('/catalogo?pagina=3');
  });

  it('tipo novo volta a primeira pagina e hardware abre o filtro de categoria', async () => {
    await abrir('/catalogo?pagina=2');
    await responder(resultado([PRODUTO], 1, 3, 41));

    [...tela.querySelectorAll('[aria-label="Tipo de produto"] button')].find((b) => b.textContent?.includes('Hardware'))!
      .dispatchEvent(new MouseEvent('click'));
    await harness.fixture.whenStable();

    expect(url()).toBe('/catalogo?tipo=HARDWARE');
    expect(tela.querySelector('#categoria')).not.toBeNull();
    expect(tela.querySelector('[aria-pressed="true"]')?.textContent?.trim()).toBe('Hardware');
  });

  it('voltar no navegador devolve o termo ao campo', async () => {
    await abrir('/catalogo?nome=hollow');
    await harness.navigateByUrl('/catalogo?nome=celeste');

    expect(tela.querySelector<HTMLInputElement>('#busca')?.value).toBe('celeste');
  });
});
