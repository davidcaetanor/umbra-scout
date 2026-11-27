import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiConfiguration } from '../../../api/api-configuration';
import { MonitoramentoResponse } from '../../../api/models/monitoramento-response';
import { CONFIGURACAO_AMBIENTE } from '../../../core/ambiente/configuracao-ambiente.token';
import { erroInterceptor } from '../../../core/http/erro.interceptor';
import { MeusMonitoramentosService } from './meus-monitoramentos.service';

const API = 'http://api.teste';

function monitoramento(parcial: Partial<MonitoramentoResponse> = {}): MonitoramentoResponse {
  return {
    id: 1,
    produtoId: 10,
    nome: 'Hollow Knight: Silksong',
    imagemUrl: null,
    precoAlvoCentavos: 8000,
    ativo: true,
    criadoEm: '2026-11-20T10:00:00Z',
    melhorPreco: {
      coletadoEm: '2026-11-24T10:00:00Z',
      descontoPct: null,
      expiraEm: null,
      gratuito: false,
      loja: 'Steam',
      situacao: 'vigente',
      url: 'https://loja.exemplo/jogo',
      valorCentavos: 6990,
      valorOriginalCentavos: null,
      verificadoEm: '2026-11-24T10:00:00Z',
    },
    distanciaDoAlvoCentavos: -1010,
    alvoAtingido: true,
    aparentementeDescontinuado: false,
    ultimoAvisoEm: null,
    precoAvisadoCentavos: null,
    ...parcial,
  };
}

function pagina(conteudo: MonitoramentoResponse[], numero: number, totalPaginas: number) {
  return { conteudo, pagina: numero, tamanho: 20, totalElementos: conteudo.length, totalPaginas };
}

describe('MeusMonitoramentosService', () => {
  let http: HttpTestingController;
  let servico: MeusMonitoramentosService;

  beforeEach(() => {
    const api = new ApiConfiguration();
    api.rootUrl = API;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroInterceptor])),
        provideHttpClientTesting(),
        { provide: ApiConfiguration, useValue: api },
        { provide: CONFIGURACAO_AMBIENTE, useValue: { apiUrl: API, loginGoogle: false } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    servico = TestBed.inject(MeusMonitoramentosService);
  });

  afterEach(() => http.verify());

  it('le todas as paginas para contar ativos e pausados de verdade', async () => {
    const resposta = servico.listarTodos();
    http.expectOne(`${API}/monitoramentos?page=0&size=20`).flush(pagina([monitoramento({ id: 1 })], 0, 2));
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne(`${API}/monitoramentos?page=1&size=20`).flush(pagina([monitoramento({ id: 2, ativo: false })], 1, 2));

    const itens = await resposta;

    expect(itens.map((item) => item.id)).toEqual([1, 2]);
  });

  it('item pausado nunca chega como alvo atingido', async () => {
    const resposta = servico.listarTodos();
    http.expectOne(`${API}/monitoramentos?page=0&size=20`).flush(pagina([monitoramento({ ativo: false })], 0, 1));

    const [item] = await resposta;

    expect(item.alvoAtingido).toBe(false);
  });

  it('foto da Kabum vira capa de hardware e falta de imagem fica sem capa', async () => {
    const resposta = servico.listarTodos();
    http.expectOne(`${API}/monitoramentos?page=0&size=20`).flush(
      pagina(
        [
          monitoramento({ id: 1, imagemUrl: 'https://images.kabum.com.br/produtos/fotos/1/1_g.jpg' }),
          monitoramento({ id: 2, imagemUrl: null, melhorPreco: null, distanciaDoAlvoCentavos: null, alvoAtingido: false }),
        ],
        0,
        1,
      ),
    );

    const [hardware, semCapa] = await resposta;

    expect(hardware.tipo).toBe('HARDWARE');
    expect(semCapa.capa).toBeNull();
    expect(semCapa.melhorPreco).toBeNull();
  });

  it('pausa pelo PATCH e devolve o resumo atualizado', async () => {
    const resposta = servico.definirAtivo(1, false);
    const requisicao = http.expectOne(`${API}/monitoramentos/1`);
    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body).toEqual({ ativo: false });
    requisicao.flush(monitoramento({ ativo: false }));

    expect((await resposta).ativo).toBe(false);
  });

  it('altera o alvo pelo PATCH so com o preco', async () => {
    const resposta = servico.alterarAlvo(1, 6000);
    const requisicao = http.expectOne(`${API}/monitoramentos/1`);
    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body).toEqual({ precoAlvoCentavos: 6000 });
    requisicao.flush(monitoramento({ precoAlvoCentavos: 6000, alvoAtingido: false }));

    expect((await resposta).precoAlvoCentavos).toBe(6000);
  });

  it('remover algo que ja nao existe conta como removido', async () => {
    const resposta = servico.remover(1);
    http
      .expectOne(`${API}/monitoramentos/1`)
      .flush({ status: 404, titulo: 'Não encontrado' }, { status: 404, statusText: 'Not Found' });

    await expect(resposta).resolves.toBeUndefined();
  });

  it('outra falha ao remover sobe para a tela', async () => {
    const resposta = servico.remover(1);
    http.expectOne(`${API}/monitoramentos/1`).flush(null, { status: 500, statusText: 'Erro' });

    await expect(resposta).rejects.toBeDefined();
  });
});
