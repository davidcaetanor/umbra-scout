import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PaginaNaoEncontradaPage } from './pagina-nao-encontrada.page';

describe('PaginaNaoEncontradaPage', () => {
  async function abrir(): Promise<HTMLElement> {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(PaginaNaoEncontradaPage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('mostra o Umbra perdido, uma frase e as duas saidas, sem codigo tecnico', async () => {
    const tela = await abrir();

    expect(tela.querySelector('h1')?.textContent?.trim()).toBe('Página não encontrada');
    expect(tela.querySelector('umbra-mascote .sprite')?.getAttribute('style')).toContain('perdido-south');
    expect([...tela.querySelectorAll('a')].map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Ir para o catálogo', '/catalogo'],
      ['Buscar um jogo', '/catalogo#busca'],
    ]);
    expect(tela.textContent).not.toMatch(/404/);
  });
});
