import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BalaoFalaComponent, LadoDaPonta } from './balao-fala.component';

@Component({
  imports: [BalaoFalaComponent],
  template: `
    <umbra-balao-fala [ponta]="ponta()" [fechavel]="fechavel()" (fechado)="fechou = true">
      <strong>3 alvos atingidos</strong> desde sua última visita.
    </umbra-balao-fala>
  `,
})
class HospedeiroComponent {
  readonly ponta = signal<LadoDaPonta>('direita');
  readonly fechavel = signal(true);
  fechou = false;
}

describe('BalaoFalaComponent', () => {
  function montar() {
    const fixture = TestBed.createComponent(HospedeiroComponent);
    fixture.detectChanges();
    return { fixture, raiz: fixture.nativeElement as HTMLElement };
  }

  it('mostra o conteúdo projetado e anuncia a novidade ao leitor de tela', () => {
    const { raiz } = montar();
    const balao = raiz.querySelector('[role="status"]') as HTMLElement;
    expect(balao.getAttribute('aria-live')).toBe('polite');
    expect(balao.textContent).toContain('3 alvos atingidos');
  });

  it('fecha pelo botão com nome acessível e avisa quem usa', () => {
    const { fixture, raiz } = montar();
    const fechar = raiz.querySelector('button[aria-label="Fechar aviso"]') as HTMLButtonElement;
    fechar.click();
    expect(fixture.componentInstance.fechou).toBe(true);
  });

  it('aponta para o lado do Umbra', () => {
    const { fixture, raiz } = montar();
    expect(raiz.querySelector('.balao')?.classList).toContain('ponta-direita');

    fixture.componentInstance.ponta.set('esquerda');
    fixture.detectChanges();
    expect(raiz.querySelector('.balao')?.classList).toContain('ponta-esquerda');
  });

  it('dispensa o botão de fechar quando o aviso não é dispensável', () => {
    const { fixture, raiz } = montar();
    fixture.componentInstance.fechavel.set(false);
    fixture.detectChanges();
    expect(raiz.querySelector('button')).toBeNull();
  });
});
