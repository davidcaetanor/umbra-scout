import { TestBed } from '@angular/core/testing';
import { LadoDaTela, MascoteDestaqueComponent } from './mascote-destaque.component';

describe('MascoteDestaqueComponent', () => {
  function montar(lado?: LadoDaTela): HTMLElement {
    const fixture = TestBed.createComponent(MascoteDestaqueComponent);
    if (lado) {
      fixture.componentRef.setInput('lado', lado);
    }
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('usa a arte que olha para a direita no lado esquerdo da tela', () => {
    expect(montar().querySelector('img')?.getAttribute('src')).toBe('mascote/destaque/umbra-destaque-256.png');
  });

  it('usa a arte espelhada redesenhada no lado direito, nunca uma inversão por CSS', () => {
    const raiz = montar('direita');
    const imagem = raiz.querySelector('img') as HTMLImageElement;
    expect(imagem.getAttribute('src')).toBe('mascote/destaque/umbra-destaque-espelhado-256.png');
    expect(imagem.style.transform).toBe('');
  });

  it('mostra a arte em escala 1x, sem a sobra abaixo dos pés e sem texto alternativo', () => {
    const raiz = montar();
    const moldura = raiz.querySelector('.moldura') as HTMLElement;
    expect(moldura.style.width).toBe('256px');
    expect(moldura.style.height).toBe('245px');
    expect(raiz.querySelector('img')?.getAttribute('alt')).toBe('');
    expect(raiz.getAttribute('aria-hidden')).toBe('true');
  });
});
