import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CONFIGURACAO_AMBIENTE } from '../ambiente';
import { CasoDeErro, ErroDaApi } from '../erro';
import { TokenDaSessao } from '../sessao';
import { RELOGIO } from '../sessao/relogio';
import { AGORA, tokenQueExpiraEm } from '../sessao/token-de-teste';
import { autenticacaoInterceptor } from './autenticacao.interceptor';
import { PRAZO_DA_API_EM_MS, erroInterceptor } from './erro.interceptor';

const API = 'http://api.teste';

describe('erroInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroInterceptor, autenticacaoInterceptor])),
        provideHttpClientTesting(),
        { provide: CONFIGURACAO_AMBIENTE, useValue: { apiUrl: API, loginGoogle: false } },
        { provide: RELOGIO, useValue: () => AGORA },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  function pedir(url: string): Promise<unknown> {
    return firstValueFrom(http.get(url)).catch((erro: unknown) => erro);
  }

  async function casoDoPedido(pedido: Promise<unknown>): Promise<CasoDeErro> {
    const erro = await pedido;
    expect(erro).toBeInstanceOf(ErroDaApi);
    return (erro as ErroDaApi).caso;
  }

  it('converte a falha da API em ErroDaApi', async () => {
    const pedido = pedir(`${API}/produtos`);
    backend.expectOne(`${API}/produtos`).error(new ProgressEvent('error'), { status: 0 });

    expect(await casoDoPedido(pedido)).toEqual({ tipo: 'rede' });
  });

  it('nao converte chamada que nao e da API', async () => {
    const pedido = pedir('/config.json');
    backend.expectOne('/config.json').flush(null, { status: 500, statusText: 'erro' });

    expect(await pedido).not.toBeInstanceOf(ErroDaApi);
  });

  it('desiste depois do prazo e cancela a requisicao', async () => {
    vi.useFakeTimers();
    const pedido = pedir(`${API}/produtos`);
    const requisicao = backend.expectOne(`${API}/produtos`);

    await vi.advanceTimersByTimeAsync(PRAZO_DA_API_EM_MS);

    expect(await casoDoPedido(pedido)).toEqual({ tipo: 'tempo-esgotado' });
    expect(requisicao.cancelled).toBe(true);
  });

  it('o 401 ainda leva ao login, porque o de autenticacao ve a resposta antes da conversao', async () => {
    const token = TestBed.inject(TokenDaSessao);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    token.definir(tokenQueExpiraEm(3_600_000));
    const pedido = pedir(`${API}/monitoramentos`);
    backend.expectOne(`${API}/monitoramentos`).flush(null, { status: 401, statusText: 'erro' });

    expect(await casoDoPedido(pedido)).toEqual({ tipo: 'sessao-encerrada' });
    expect(token.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledOnce();
  });

  it('o 401 do login chega ao formulario como credencial recusada', async () => {
    const pedido = pedir(`${API}/auth/login`);
    backend.expectOne(`${API}/auth/login`).flush(null, { status: 401, statusText: 'erro' });

    expect(await casoDoPedido(pedido)).toEqual({ tipo: 'credencial-recusada' });
  });
});
