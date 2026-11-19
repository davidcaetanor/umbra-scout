import { TestBed } from '@angular/core/testing';
import { ApiConfiguration } from '../../api/api-configuration';
import { CONFIGURACAO_AMBIENTE } from './configuracao-ambiente.token';
import { provideConfiguracaoAmbiente } from './provide-configuracao-ambiente';

describe('provideConfiguracaoAmbiente', () => {
  const configuracao = { apiUrl: 'https://api.umbra.davidcaetano.tech', loginGoogle: false };

  beforeEach(() => TestBed.configureTestingModule({ providers: [provideConfiguracaoAmbiente(configuracao)] }));

  it('disponibiliza a configuracao pelo token', () => {
    expect(TestBed.inject(CONFIGURACAO_AMBIENTE)).toBe(configuracao);
  });

  it('aponta o cliente gerado para a origem da API', () => {
    expect(TestBed.inject(ApiConfiguration).rootUrl).toBe(configuracao.apiUrl);
  });
});
