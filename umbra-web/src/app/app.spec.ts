import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  it('renderiza a saida de rotas', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('router-outlet')).not.toBeNull();
  });
});
