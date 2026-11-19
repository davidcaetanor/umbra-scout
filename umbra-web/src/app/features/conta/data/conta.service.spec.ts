import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiConfiguration } from '../../../api/api-configuration';
import { ContaService } from './conta.service';

const API = 'http://api.teste';
const USUARIO = {
  id: 7,
  email: 'ana@exemplo.com',
  nomeExibicao: 'Ana Maria da Silva',
  cep: '01310100',
  cidade: 'São Paulo',
  uf: 'SP',
  criadoEm: '2026-11-19T12:00:00Z',
};

describe('ContaService', () => {
  let http: HttpTestingController;
  let conta: ContaService;

  beforeEach(() => {
    const api = new ApiConfiguration();
    api.rootUrl = API;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ApiConfiguration, useValue: api }],
    });
    http = TestBed.inject(HttpTestingController);
    conta = TestBed.inject(ContaService);
  });

  afterEach(() => http.verify());

  it('adapta o usuario para a tela com nome completo, regiao e data por extenso', async () => {
    const dados = conta.carregar();
    http.expectOne(`${API}/usuarios/eu`).flush(USUARIO);

    expect(await dados).toEqual({
      nome: 'Ana Maria da Silva',
      email: 'ana@exemplo.com',
      regiao: 'São Paulo/SP',
      membroDesde: '19 de novembro de 2026',
    });
  });

  it('deixa a regiao vazia sem cidade e UF', async () => {
    const dados = conta.carregar();
    http.expectOne(`${API}/usuarios/eu`).flush({ ...USUARIO, cep: '99999999', cidade: null, uf: null });

    expect((await dados).regiao).toBeNull();
  });
});
