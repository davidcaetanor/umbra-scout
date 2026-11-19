import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { criarAppConfig } from './app.config';
import { CONFIGURACAO_AMBIENTE } from './core/ambiente';

describe('criarAppConfig', () => {
  it('registra a configuracao do ambiente e o roteador', () => {
    const configuracao = { apiUrl: 'http://localhost:8080', loginGoogle: true };
    TestBed.configureTestingModule({ providers: criarAppConfig(configuracao).providers });

    expect(TestBed.inject(CONFIGURACAO_AMBIENTE)).toBe(configuracao);
    expect(TestBed.inject(Router)).toBeTruthy();
  });
});
