import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiConfiguration } from '../../../api/api-configuration';
import { OfertaResponse } from '../../../api/models/oferta-response';
import { MonitoramentoDoProdutoService } from './monitoramento-do-produto.service';
import { ProdutoService } from './produto.service';

const API = 'http://api.teste';

function oferta(parcial: Partial<OfertaResponse> = {}): OfertaResponse {
  return {
    coletadoEm: '2026-11-19T10:00:00Z',
    descontoPct: null,
    expiraEm: null,
    gratuito: false,
    loja: 'Steam',
    promocaoEncerrada: false,
    url: 'https://loja.exemplo/jogo',
    valorCentavos: 9990,
    valorOriginalCentavos: null,
    ...parcial,
  };
}

describe('serviços do detalhe do produto', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    const api = new ApiConfiguration();
    api.rootUrl = API;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ApiConfiguration, useValue: api }],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('detalha o produto com ofertas da mais barata para a mais cara e a categoria por extenso', async () => {
    const resposta = TestBed.inject(ProdutoService).detalhar(42);
    http.expectOne(`${API}/produtos/42`).flush({
      id: 42,
      nome: 'Placa',
      tipo: 'HARDWARE',
      categoria: 'GPU',
      imagemUrl: null,
      melhorPreco: oferta({ loja: 'KaBuM!', valorCentavos: 100 }),
      ofertas: [
        oferta({ loja: 'Terabyte', valorCentavos: 200, promocaoEncerrada: true, descontoPct: 10, valorOriginalCentavos: 300 }),
        oferta({ loja: 'KaBuM!', valorCentavos: 100 }),
      ],
    });

    const produto = await resposta;

    expect(produto.categoria).toBe('Placa de vídeo');
    expect(produto.melhorOferta?.loja).toBe('KaBuM!');
    expect(produto.ofertas.map((item) => item.loja)).toEqual(['KaBuM!', 'Terabyte']);
    expect(produto.ofertas[1].preco.descontoPct).toBeNull();
  });

  it('trata oferta grátis como valor zero', async () => {
    const resposta = TestBed.inject(ProdutoService).detalhar(1);
    http.expectOne(`${API}/produtos/1`).flush({
      id: 1,
      nome: 'Jogo',
      tipo: 'JOGO',
      categoria: null,
      imagemUrl: null,
      melhorPreco: oferta({ gratuito: true, valorCentavos: 0 }),
      ofertas: [oferta({ gratuito: true, valorCentavos: 0 })],
    });

    const produto = await resposta;

    expect(produto.melhorOferta).toMatchObject({ gratuito: true, valorCentavos: 0 });
    expect(produto.melhorOferta?.preco.atual).toBe('Grátis');
    expect(produto.categoria).toBeNull();
  });

  it('pede o histórico no período e converte os instantes', async () => {
    const resposta = TestBed.inject(ProdutoService).historico(42, '30d');
    http.expectOne(`${API}/produtos/42/historico?periodo=30d`).flush({
      consultadoEm: '2026-11-21T12:00:00Z',
      observadoDesde: null,
      periodo: '30d',
      faixas: [1, 2, 3],
      melhorPreco: [{ instante: '2026-11-20T00:00:00Z', valorCentavos: null }],
      porLoja: [{ loja: 'Steam', pontos: [{ instante: '2026-11-20T00:00:00Z', valorCentavos: 500 }] }],
      minimo: { valorCentavos: 900, gratuito: true, loja: 'Epic', em: '2026-11-10T00:00:00Z' },
      variacao: { diferencaCentavos: null, percentualAcimaDoMinimo: null, precoAtualCentavos: null },
    });

    const historico = await resposta;

    expect(historico.consultadoEm).toBe(Date.parse('2026-11-21T12:00:00Z'));
    expect(historico.observadoDesde).toBeNull();
    expect(historico.porLoja[0].pontos[0].valorCentavos).toBe(500);
    expect(historico.minimo).toEqual({ valorCentavos: 0, loja: 'Epic', em: Date.parse('2026-11-10T00:00:00Z') });
  });

  it('busca o monitoramento do produto pelo filtro e devolve nulo se não houver', async () => {
    const servico = TestBed.inject(MonitoramentoDoProdutoService);
    const pagina = (conteudo: unknown[]) => ({ conteudo, pagina: 0, tamanho: 1, totalElementos: conteudo.length, totalPaginas: 1 });

    const encontrado = servico.buscar(42);
    http.expectOne(`${API}/monitoramentos?produtoId=42&size=1`).flush(pagina([{ id: 9, precoAlvoCentavos: 14990, ativo: true }]));
    expect(await encontrado).toEqual({ id: 9, precoAlvoCentavos: 14990 });

    const ausente = servico.buscar(43);
    http.expectOne(`${API}/monitoramentos?produtoId=43&size=1`).flush(pagina([]));
    expect(await ausente).toBeNull();
  });

  it('cria o monitoramento com produto e alvo em centavos', async () => {
    const criado = TestBed.inject(MonitoramentoDoProdutoService).criar(42, 14990);
    const pedido = http.expectOne(`${API}/monitoramentos`);

    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({ produtoId: 42, precoAlvoCentavos: 14990 });
    pedido.flush({ id: 9, precoAlvoCentavos: 14990 });
    expect(await criado).toEqual({ id: 9, precoAlvoCentavos: 14990 });
  });
});
