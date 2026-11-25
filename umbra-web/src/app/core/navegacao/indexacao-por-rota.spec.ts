import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { Router, provideRouter } from '@angular/router';
import { SEM_INDEXACAO, provideIndexacaoPorRota } from './indexacao-por-rota';

@Component({ template: '' })
class PaginaVazia {}

describe('provideIndexacaoPorRota', () => {
  let router: Router;
  let documento: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: PaginaVazia },
          { path: 'catalogo', component: PaginaVazia },
          { path: 'conta', data: SEM_INDEXACAO, children: [{ path: '', component: PaginaVazia }] },
          { path: 'entrar', component: PaginaVazia, data: SEM_INDEXACAO },
        ]),
        provideIndexacaoPorRota(),
      ],
    });
    router = TestBed.inject(Router);
    documento = TestBed.inject(DOCUMENT);
  });

  afterEach(() => documento.head.querySelector('meta[name="robots"]')?.remove());

  const robots = () => documento.head.querySelector('meta[name="robots"]')?.getAttribute('content') ?? null;

  it('marca noindex na rota privada', async () => {
    await router.navigateByUrl('/entrar');
    expect(robots()).toBe('noindex');
  });

  it('herda o noindex da rota pai', async () => {
    await router.navigateByUrl('/conta');
    expect(robots()).toBe('noindex');
  });

  it('não marca a rota pública', async () => {
    await router.navigateByUrl('/catalogo');
    expect(robots()).toBeNull();
  });

  it('retira o noindex ao voltar para uma rota pública', async () => {
    await router.navigateByUrl('/conta');
    await router.navigateByUrl('/');
    expect(robots()).toBeNull();
  });
});
