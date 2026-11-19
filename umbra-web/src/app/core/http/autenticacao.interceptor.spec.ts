import type { MockInstance } from 'vitest';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CONFIGURACAO_AMBIENTE } from '../ambiente';
import { TokenDaSessao } from '../sessao';
import { RELOGIO } from '../sessao/relogio';
import { AGORA, tokenQueExpiraEm } from '../sessao/token-de-teste';
import { autenticacaoInterceptor } from './autenticacao.interceptor';

const API = 'http://api.teste';

describe('autenticacaoInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let token: TokenDaSessao;
  let navegar: MockInstance<Router['navigate']>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([autenticacaoInterceptor])),
        provideHttpClientTesting(),
        { provide: CONFIGURACAO_AMBIENTE, useValue: { apiUrl: API, loginGoogle: false } },
        { provide: RELOGIO, useValue: () => AGORA },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    token = TestBed.inject(TokenDaSessao);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  async function falhar(url: string, status: number): Promise<unknown> {
    const pedido = firstValueFrom(http.get(url)).catch((erro: unknown) => erro);
    backend.expectOne(url).flush(null, { status, statusText: 'erro' });
    return pedido;
  }

  it('anexa o token nas chamadas para a API', () => {
    const vigente = tokenQueExpiraEm(3_600_000);
    token.definir(vigente);

    void firstValueFrom(http.get(`${API}/monitoramentos`));

    expect(backend.expectOne(`${API}/monitoramentos`).request.headers.get('Authorization')).toBe(`Bearer ${vigente}`);
  });

  it('nunca envia o token para outra origem', () => {
    token.definir(tokenQueExpiraEm(3_600_000));

    void firstValueFrom(http.get('/config.json'));
    void firstValueFrom(http.get('http://api.teste.malicioso.example/x'));

    expect(backend.expectOne('/config.json').request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne('http://api.teste.malicioso.example/x').request.headers.has('Authorization')).toBe(false);
  });

  it('segue sem cabecalho quando nao ha sessao', () => {
    void firstValueFrom(http.get(`${API}/produtos`));

    expect(backend.expectOne(`${API}/produtos`).request.headers.has('Authorization')).toBe(false);
  });

  it('no 401 com sessao limpa o token e leva ao login avisando que expirou', async () => {
    token.definir(tokenQueExpiraEm(3_600_000));

    const erro = await falhar(`${API}/monitoramentos`, 401);

    expect(erro).toBeInstanceOf(HttpErrorResponse);
    expect(token.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { returnUrl: '/', motivo: 'expirada' } });
  });

  it('no 401 sem sessao leva ao login sem falar em expiracao', async () => {
    await falhar(`${API}/monitoramentos`, 401);

    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { returnUrl: '/' } });
  });

  it('deixa o 401 das rotas de autenticacao para a tela tratar', async () => {
    token.definir(tokenQueExpiraEm(3_600_000));

    await falhar(`${API}/auth/login`, 401);

    expect(navegar).not.toHaveBeenCalled();
    expect(token.autenticado()).toBe(true);
  });

  it('nao trata outros erros', async () => {
    token.definir(tokenQueExpiraEm(3_600_000));

    await falhar(`${API}/monitoramentos`, 500);

    expect(navegar).not.toHaveBeenCalled();
    expect(token.autenticado()).toBe(true);
  });
});
