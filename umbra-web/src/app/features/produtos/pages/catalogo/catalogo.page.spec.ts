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

const PRODUTO: ProdutoDoCatalogo = {
  id: 1,
  nome: 'Hollow Knight',
  tipo: 'JOGO',
  imagem: null,
  categoria: null,
  preco: { atual: 'R$ 23,99', original: 'R$ 47,99', descontoPct: 50, loja: 'Steam' },
};

const EXTERNO: ProdutoExterno = {
  gid: 'g1',
  nome: 'Hollow Edition',
  imagem: null,
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

  it('mostra o esqueleto, depois os cards e anuncia a contagem', async () => {
    await abrir();

    expect(tela.querySelector('[role="status"] .sr-only')?.textContent).toContain('Carregando produtos');

    await responder(resultado([PRODUTO]));

    expect(tela.querySelector('umbra-card-produto h3')?.textContent?.trim()).toBe('Hollow Knight');
    expect(tela.textContent).toContain('1 produto encontrado');
    expect(umbra()[0]).toContain('observando');
  });

  it('le os filtros da URL e ignora os invalidos', async () => {
    await abrir('/catalogo?nome=hollow&tipo=FILME&pagina=2');

    expect(catalogo.buscas[0]).toEqual({ termo: 'hollow', tipo: null, categoria: null, ordenacao: 'nome', pagina: 1 });
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
