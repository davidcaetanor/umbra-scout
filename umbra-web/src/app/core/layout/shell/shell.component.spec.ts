import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideFocoNaTrocaDeRota } from '../../navegacao/foco-na-troca-de-rota';
import { ShellComponent } from './shell.component';

@Component({ selector: 'umbra-pagina-teste', template: '<h1>Destino</h1>' })
class PaginaTesteComponent {}

describe('ShellComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'catalogo', component: PaginaTesteComponent },
          { path: 'monitoramentos', component: PaginaTesteComponent },
        ]),
        provideFocoNaTrocaDeRota(),
      ],
    });
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  async function montar() {
    const fixture = TestBed.createComponent(ShellComponent);
    document.body.appendChild(fixture.nativeElement);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/catalogo');
    await fixture.whenStable();
    return { fixture, router, raiz: fixture.nativeElement as HTMLElement };
  }

  it('oferece o link de salto apontando para o conteúdo principal', async () => {
    const { raiz } = await montar();
    const salto = raiz.querySelector('a[href="#conteudo"]');
    expect(salto?.textContent?.trim()).toBe('Pular para o conteúdo');
    expect(raiz.querySelector('main#conteudo')).not.toBeNull();
  });

  it('marca o item ativo com aria-current nas duas navegações', async () => {
    const { raiz } = await montar();
    const atuais = [...raiz.querySelectorAll('[aria-current="page"]')].map((e) => e.textContent?.trim());
    expect(atuais).toEqual(['Catálogo', 'Catálogo']);
  });

  it('leva o foco ao título da página depois de navegar', async () => {
    const { fixture, router } = await montar();
    await router.navigateByUrl('/monitoramentos');
    await fixture.whenStable();
    await new Promise((resolver) => setTimeout(resolver));
    expect(document.activeElement?.tagName).toBe('H1');
    expect(document.activeElement?.getAttribute('tabindex')).toBe('-1');
  });
});
