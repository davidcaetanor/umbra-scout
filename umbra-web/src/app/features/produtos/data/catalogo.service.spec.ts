import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ApiConfiguration } from '../../../api/api-configuration';
import { OfertaResponse } from '../../../api/models/oferta-response';
import { CatalogoService } from './catalogo.service';
import { FiltroDaBusca } from './filtro-da-busca';

const API = 'http://api.teste';
const FILTRO: FiltroDaBusca = {
  termo: null,
  tipo: null,
  categoria: null,
  ordenacao: 'nome',
  precoMin: null,
  precoMax: null,
  descontoMin: null,
  loja: null,
  pagina: 0,
};

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

function resposta(conteudo: unknown[], externos: unknown[] = []) {
  return { conteudo, externos, pagina: 0, tamanho: 20, totalElementos: conteudo.length, totalPaginas: 1 };
}

describe('CatalogoService', () => {
  let servico: CatalogoService;
  let http: HttpTestingController;

  beforeEach(() => {
    const api = new ApiConfiguration();
    api.rootUrl = API;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ApiConfiguration, useValue: api }],
    });
    servico = TestBed.inject(CatalogoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function pedido(trecho: string) {
    return http.expectOne((req) => req.urlWithParams.startsWith(`${API}/produtos`) && req.urlWithParams.includes(trecho));
  }

  it('envia so os filtros informados', () => {
    void firstValueFrom(servico.buscar({ ...FILTRO, termo: 'hollow', tipo: 'JOGO', pagina: 2 }));

    const params = pedido('nome=hollow').request.params;
    expect(params.get('tipo')).toBe('JOGO');
    expect(params.get('page')).toBe('2');
    expect(params.get('ordenacao')).toBe('nome');
    expect(params.has('categoria')).toBe(false);
    expect(params.has('incluirExterno')).toBe(false);
  });

  it('converte a faixa de preco para centavos e envia desconto e loja', () => {
    void firstValueFrom(
      servico.buscar({ ...FILTRO, tipo: 'JOGO', ordenacao: 'melhoresOfertas', precoMin: 10, precoMax: 250, descontoMin: 50, loja: 'GOG' }),
    );

    const params = pedido('ordenacao=melhoresOfertas').request.params;
    expect(params.get('precoMin')).toBe('1000');
    expect(params.get('precoMax')).toBe('25000');
    expect(params.get('descontoMin')).toBe('50');
    expect(params.get('loja')).toBe('GOG');
  });

  it('busca os destaques e adapta como os produtos do catalogo', async () => {
    const destaques = firstValueFrom(servico.destaques());
    http
      .expectOne(`${API}/produtos/destaques`)
      .flush({ conteudo: [{ id: 7, nome: 'Jogo', tipo: 'JOGO', categoria: null, imagemUrl: null, melhorPreco: oferta() }] });

    expect(await destaques).toEqual([
      {
        id: 7,
        nome: 'Jogo',
        tipo: 'JOGO',
        capa: null,
        categoria: null,
        preco: { atual: 'R$ 99,90', original: null, descontoPct: null, loja: 'Steam' },
      },
    ]);
  });

  it('adapta o produto para a tela: imagem pela origem, categoria legivel e preco em reais', async () => {
    const resultado = firstValueFrom(servico.buscar(FILTRO));
    pedido('').flush(
      resposta([
        {
          id: 1,
          nome: 'Placa',
          tipo: 'HARDWARE',
          categoria: 'GPU',
          imagemUrl: 'https://assets.isthereanydeal.com/a/boxart.jpg',
          melhorPreco: oferta({ valorCentavos: 150000, valorOriginalCentavos: 200000, descontoPct: 25, loja: 'Kabum' }),
        },
        { id: 2, nome: 'Esgotado', tipo: 'JOGO', categoria: null, imagemUrl: null, melhorPreco: null },
      ]),
    );

    expect((await resultado).produtos).toEqual([
      {
        id: 1,
        nome: 'Placa',
        tipo: 'HARDWARE',
        capa: {
          endereco: '/imagens/itad/a/banner300.jpg',
          alternativas: '/imagens/itad/a/banner300.jpg 300w, /imagens/itad/a/banner400.jpg 400w',
          reserva: '/imagens/itad/a/boxart.jpg',
        },
        categoria: 'Placa de vídeo',
        preco: { atual: 'R$ 1.500,00', original: 'R$ 2.000,00', descontoPct: 25, loja: 'Kabum' },
      },
      { id: 2, nome: 'Esgotado', tipo: 'JOGO', capa: null, categoria: null, preco: null },
    ]);
  });

  it('gratis e promocao encerrada nao mostram desconto', async () => {
    const resultado = firstValueFrom(servico.buscar(FILTRO));
    pedido('').flush(
      resposta([
        { id: 1, nome: 'A', tipo: 'JOGO', categoria: null, imagemUrl: null, melhorPreco: oferta({ gratuito: true, valorCentavos: 0 }) },
        {
          id: 2,
          nome: 'B',
          tipo: 'JOGO',
          categoria: null,
          imagemUrl: null,
          melhorPreco: oferta({ promocaoEncerrada: true, descontoPct: 50, valorOriginalCentavos: 19980 }),
        },
      ]),
    );

    const [gratis, encerrada] = (await resultado).produtos;
    expect(gratis.preco).toMatchObject({ atual: 'Grátis', original: null, descontoPct: null });
    expect(encerrada.preco).toMatchObject({ atual: 'R$ 99,90', original: null, descontoPct: null });
  });

  it('busca fora do catalogo sempre na primeira pagina e devolve os externos', async () => {
    const externos = firstValueFrom(servico.buscarForaDoCatalogo({ ...FILTRO, termo: 'hollow', pagina: 3 }));
    const requisicao = pedido('incluirExterno=true');
    expect(requisicao.request.params.get('page')).toBe('0');
    requisicao.flush(resposta([], [{ gid: 'g1', nome: 'Hollow', imagemUrl: null, melhorPreco: oferta() }]));

    expect(await externos).toEqual([
      {
        gid: 'g1',
        nome: 'Hollow',
        capa: null,
        preco: { atual: 'R$ 99,90', original: null, descontoPct: null, loja: 'Steam' },
        enderecoDaLoja: 'https://loja.exemplo/jogo',
      },
    ]);
  });

  it('cancelar a inscricao cancela a requisicao, base do switchMap da busca', () => {
    const inscricao = servico.buscar(FILTRO).subscribe();
    const requisicao = pedido('');

    inscricao.unsubscribe();

    expect(requisicao.cancelled).toBe(true);
  });
});
