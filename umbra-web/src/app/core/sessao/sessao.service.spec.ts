import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { erroInterceptor } from '../http';
import { ApiConfiguration } from '../../api/api-configuration';
import { CONFIGURACAO_AMBIENTE } from '../ambiente';
import { RELOGIO } from './relogio';
import { SessaoService } from './sessao.service';
import { AGORA, tokenQueExpiraEm } from './token-de-teste';

const API = 'http://api.teste';
const USUARIO = {
  id: 7,
  email: 'ana@exemplo.com',
  nomeExibicao: 'ana maria da silva',
  cep: null,
  cidade: null,
  uf: null,
  criadoEm: '2026-11-01T00:00:00Z',
};

describe('SessaoService', () => {
  let http: HttpTestingController;
  let sessao: SessaoService;

  beforeEach(() => {
    localStorage.clear();
    const api = new ApiConfiguration();
    api.rootUrl = API;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroInterceptor])),
        provideHttpClientTesting(),
        { provide: ApiConfiguration, useValue: api },
        { provide: CONFIGURACAO_AMBIENTE, useValue: { apiUrl: API, loginGoogle: false } },
        { provide: RELOGIO, useValue: () => AGORA },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
  });

  afterEach(() => http.verify());

  const esperarPromessas = (): Promise<void> => new Promise((resolver) => setTimeout(resolver));

  async function responderUsuario(status: number): Promise<void> {
    TestBed.tick();
    const requisicao = http.expectOne(`${API}/usuarios/eu`);
    if (status === 200) {
      requisicao.flush(USUARIO);
    } else {
      requisicao.flush(null, { status, statusText: 'erro' });
    }
    await esperarPromessas();
  }

  it('busca o usuario ao entrar e o esquece ao sair', async () => {
    sessao.entrar(tokenQueExpiraEm(3_600_000));
    await responderUsuario(200);

    expect(sessao.usuario()).toEqual({ nome: 'Ana Maria', nomeCompleto: 'ana maria da silva', email: 'ana@exemplo.com' });
    expect(sessao.autenticado()).toBe(true);

    sessao.sair();
    TestBed.tick();

    expect(sessao.usuario()).toBeNull();
    expect(sessao.autenticado()).toBe(false);
  });

  it('encerra a sessao quando a conta foi desativada', async () => {
    sessao.entrar(tokenQueExpiraEm(3_600_000));
    await responderUsuario(404);

    expect(sessao.autenticado()).toBe(false);
  });

  it('mantem o token quando a busca do usuario falha por outro motivo', async () => {
    sessao.entrar(tokenQueExpiraEm(3_600_000));
    await responderUsuario(503);

    expect(sessao.autenticado()).toBe(true);
    expect(sessao.usuario()).toBeNull();
  });

  it('ignora a resposta que chega depois de o token mudar', async () => {
    sessao.entrar(tokenQueExpiraEm(3_600_000));
    TestBed.tick();
    const primeira = http.expectOne(`${API}/usuarios/eu`);

    sessao.sair();
    TestBed.tick();
    primeira.flush(USUARIO);
    await esperarPromessas();

    expect(sessao.usuario()).toBeNull();
  });
});
