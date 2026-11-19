import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginacaoComponent } from './paginacao.component';

describe('PaginacaoComponent', () => {
  let fixture: ComponentFixture<PaginacaoComponent>;
  let destinos: number[];

  async function mostrar(pagina: number, totalDePaginas: number): Promise<HTMLElement> {
    fixture = TestBed.createComponent(PaginacaoComponent);
    destinos = [];
    fixture.componentInstance.irPara.subscribe((destino) => destinos.push(destino));
    fixture.componentRef.setInput('pagina', pagina);
    fixture.componentRef.setInput('totalDePaginas', totalDePaginas);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const botao = (raiz: HTMLElement, rotulo: string) =>
    [...raiz.querySelectorAll('button')].find((b) => b.textContent?.trim() === rotulo)!;

  it('uma pagina so nao mostra navegacao', async () => {
    expect((await mostrar(0, 1)).querySelector('nav')).toBeNull();
  });

  it('mostra a pagina humana e trava as pontas', async () => {
    const raiz = await mostrar(0, 3);

    expect(raiz.textContent).toContain('Página 1 de 3');
    expect(botao(raiz, 'Anterior').disabled).toBe(true);

    botao(raiz, 'Próxima').click();
    expect(destinos).toEqual([1]);
  });

  it('na ultima pagina so volta', async () => {
    const raiz = await mostrar(2, 3);

    expect(botao(raiz, 'Próxima').disabled).toBe(true);
    botao(raiz, 'Anterior').click();
    expect(destinos).toEqual([1]);
  });
});
