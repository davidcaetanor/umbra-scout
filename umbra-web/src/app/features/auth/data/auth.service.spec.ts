import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { erroInterceptor } from '../../../core/http';
import { ApiConfiguration } from '../../../api/api-configuration';
import { CONFIGURACAO_AMBIENTE } from '../../../core/ambiente';
import { SessaoService } from '../../../core/sessao';
import { AuthService, SAIR_PARA } from './auth.service';

const API = 'http://api.teste';
const TOKEN = { token: 'jwt', tipo: 'Bearer', expiraEm: '2026-11-19T18:00:00Z' };
const CADASTRO = { nome: 'Ana', email: 'ana@exemplo.com', senha: 'segredo123' };

describe('AuthService', () => {
  let http: HttpTestingController;
  let entrarNaSessao: ReturnType<typeof vi.fn>;
  let sairPara: ReturnType<typeof vi.fn>;

  function criar(loginGoogle = true): AuthService {
    const api = new ApiConfiguration();
    api.rootUrl = API;
    entrarNaSessao = vi.fn();
    sairPara = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroInterceptor])),
        provideHttpClientTesting(),
        { provide: ApiConfiguration, useValue: api },
        { provide: CONFIGURACAO_AMBIENTE, useValue: { apiUrl: API, loginGoogle } },
        { provide: SessaoService, useValue: { entrar: entrarNaSessao } },
        { provide: SAIR_PARA, useValue: sairPara },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  const esperar = (): Promise<void> => new Promise((resolver) => setTimeout(resolver));

  function responder(caminho: string, status: number, corpo: object | null = null): void {
    const pedido = http.expectOne(`${API}${caminho}`);
    if (status < 300) {
      pedido.flush(corpo, { status, statusText: 'ok' });
    } else {
      pedido.flush(corpo, { status, statusText: 'erro' });
    }
  }

  beforeEach(() => sessionStorage.clear());
  afterEach(() => http.verify());

  it('oferece o Google so quando o ambiente libera', () => {
    expect(criar(true).provedores).toEqual(['discord', 'google']);
    TestBed.resetTestingModule();
    expect(criar(false).provedores).toEqual(['discord']);
  });

  describe('entrar', () => {
    it('guarda o token na sessao', async () => {
      const auth = criar();
      const resultado = auth.entrar({ email: 'ana@exemplo.com', senha: 'x' });
      responder('/auth/login', 200, TOKEN);

      expect(await resultado).toBe('ok');
      expect(entrarNaSessao).toHaveBeenCalledWith('jwt');
    });

    it.each([401, 400])('trata %i como credencial recusada', async (status) => {
      const auth = criar();
      const resultado = auth.entrar({ email: 'ana@exemplo.com', senha: 'x' });
      responder('/auth/login', status);

      expect(await resultado).toBe('credenciais');
    });

    it('trata outro erro como falha', async () => {
      const auth = criar();
      const resultado = auth.entrar({ email: 'ana@exemplo.com', senha: 'x' });
      responder('/auth/login', 503);

      expect(await resultado).toBe('falha');
    });
  });

  describe('cadastrar', () => {
    it('cria a conta e entra com as mesmas credenciais', async () => {
      const auth = criar();
      const resultado = auth.cadastrar(CADASTRO);
      const pedido = http.expectOne(`${API}/auth/cadastro`);
      expect(pedido.request.body).toEqual({ nomeExibicao: 'Ana', email: 'ana@exemplo.com', senha: 'segredo123' });
      pedido.flush(null, { status: 201, statusText: 'criado' });
      await esperar();
      const login = http.expectOne(`${API}/auth/login`);
      expect(login.request.body).toEqual({ email: 'ana@exemplo.com', senha: 'segredo123' });
      login.flush(TOKEN);

      expect(await resultado).toEqual({ tipo: 'ok' });
    });

    it('avisa quando a conta foi criada mas a entrada falhou', async () => {
      const auth = criar();
      const resultado = auth.cadastrar(CADASTRO);
      responder('/auth/cadastro', 201);
      await esperar();
      responder('/auth/login', 503);

      expect(await resultado).toEqual({ tipo: 'criada-sem-entrar' });
    });

    it('reconhece email ja usado', async () => {
      const auth = criar();
      const resultado = auth.cadastrar(CADASTRO);
      responder('/auth/cadastro', 409);

      expect(await resultado).toEqual({ tipo: 'email-em-uso' });
    });

    it('traduz os erros de campo do contrato e ignora campo desconhecido', async () => {
      const auth = criar();
      const resultado = auth.cadastrar(CADASTRO);
      responder('/auth/cadastro', 400, {
        erros: [
          { campo: 'nomeExibicao', mensagem: 'é obrigatório' },
          { campo: 'cep', mensagem: 'deve ter 8 dígitos' },
        ],
      });

      expect(await resultado).toEqual({ tipo: 'invalido', erros: [{ campo: 'nome', mensagem: 'é obrigatório' }] });
    });

    it.each([
      [400, null],
      [500, null],
    ])('trata %i sem erro de campo como falha', async (status, corpo) => {
      const auth = criar();
      const resultado = auth.cadastrar(CADASTRO);
      responder('/auth/cadastro', status, corpo);

      expect(await resultado).toEqual({ tipo: 'falha' });
    });
  });

  describe('trocarCodigo', () => {
    it('troca o codigo pelo token uma vez so, mesmo pedida duas vezes', async () => {
      const auth = criar();
      const primeira = auth.trocarCodigo('abc');
      const segunda = auth.trocarCodigo('abc');
      const pedido = http.expectOne(`${API}/auth/oauth2/token`);
      expect(pedido.request.body).toEqual({ codigo: 'abc' });
      pedido.flush(TOKEN);

      expect(await primeira).toBe('ok');
      expect(await segunda).toBe('ok');
      expect(entrarNaSessao).toHaveBeenCalledTimes(1);
    });

    it.each([
      [401, 'codigo-invalido'],
      [400, 'codigo-invalido'],
      [503, 'falha'],
    ])('traduz %i em %s', async (status, esperado) => {
      const auth = criar();
      const resultado = auth.trocarCodigo('abc');
      responder('/auth/oauth2/token', status);

      expect(await resultado).toBe(esperado);
    });
  });

  describe('login externo', () => {
    it('guarda o destino e sai para o provedor pela API', () => {
      const auth = criar();

      auth.iniciarLoginExterno('discord', '/monitoramentos');

      expect(sairPara).toHaveBeenCalledWith(`${API}/oauth2/authorization/discord`);
      expect(auth.retomarDestino()).toBe('/monitoramentos');
      expect(auth.retomarDestino()).toBeNull();
    });

    it('segue sem destino quando o armazenamento recusa acesso', () => {
      const auth = criar();
      const gravar = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new DOMException('negado', 'SecurityError');
      });
      const ler = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new DOMException('negado', 'SecurityError');
      });

      auth.iniciarLoginExterno('google', '/monitoramentos');

      expect(sairPara).toHaveBeenCalledWith(`${API}/oauth2/authorization/google`);
      expect(auth.retomarDestino()).toBeNull();
      gravar.mockRestore();
      ler.mockRestore();
    });
  });
});
