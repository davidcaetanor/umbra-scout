import { TestBed } from '@angular/core/testing';
import { ListaDeMudancasComponent } from './lista-de-mudancas.component';
import { MudancaExibida } from './mudanca-exibida';

const mudanca = (data: string, menor = false): MudancaExibida => ({ data, texto: `Steam mudou em ${data}`, menor });

describe('ListaDeMudancasComponent', () => {
  async function montar(mudancas: MudancaExibida[]) {
    const fixture = TestBed.createComponent(ListaDeMudancasComponent);
    fixture.componentRef.setInput('mudancas', mudancas);
    await fixture.whenStable();
    return { fixture, tela: fixture.nativeElement as HTMLElement };
  }

  const itens = (tela: HTMLElement) => [...tela.querySelectorAll('li')].map((item) => item.textContent?.replace(/\s+/g, ' ').trim());

  it('mostra três, expande todas e recolhe', async () => {
    const { fixture, tela } = await montar([mudanca('20/11'), mudanca('17/11'), mudanca('12/11'), mudanca('07/11', true)]);
    const botao = () => tela.querySelector('button');

    expect(itens(tela)).toEqual(['20/11: Steam mudou em 20/11', '17/11: Steam mudou em 17/11', '07/11: Menor preço registrado: Steam mudou em 07/11']);
    expect(botao()?.textContent?.trim()).toBe('Ver todas (4)');
    expect(botao()?.getAttribute('aria-expanded')).toBe('false');

    botao()?.click();
    await fixture.whenStable();
    expect(itens(tela)).toHaveLength(4);
    expect(botao()?.textContent?.trim()).toBe('Ver menos');

    botao()?.click();
    await fixture.whenStable();
    expect(itens(tela)).toHaveLength(3);
  });

  it('sem mais mudanças não mostra o botão', async () => {
    const { tela } = await montar([mudanca('20/11'), mudanca('17/11')]);

    expect(tela.querySelector('button')).toBeNull();
  });
});
