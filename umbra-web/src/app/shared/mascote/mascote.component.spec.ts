import { ComponentRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MascoteComponent } from './mascote.component';
import { EstadoMascote } from './estado-mascote';

describe('MascoteComponent', () => {
  let ref: ComponentRef<MascoteComponent>;

  function sprite(): HTMLElement {
    return (ref.location.nativeElement as HTMLElement).querySelector('.sprite') as HTMLElement;
  }

  function montar(estado: EstadoMascote, extras: Record<string, unknown> = {}): void {
    const fixture = TestBed.createComponent(MascoteComponent);
    ref = fixture.componentRef;
    ref.setInput('estado', estado);
    Object.entries(extras).forEach(([nome, valor]) => ref.setInput(nome, valor));
    fixture.detectChanges();
  }

  it('fica fora da árvore de acessibilidade, porque o texto ao lado informa', () => {
    montar('observando');
    expect((ref.location.nativeElement as HTMLElement).getAttribute('aria-hidden')).toBe('true');
  });

  it('usa a tira do estado pedido em escala 2x por padrão, sem a sobra abaixo dos pés', () => {
    montar('observando');
    const estilo = sprite().style;
    expect(estilo.getPropertyValue('--sprite')).toContain('mascote/spritesheets/observando-south.png');
    expect(estilo.getPropertyValue('--largura')).toBe('184px');
    expect(estilo.getPropertyValue('--altura-visivel')).toBe('156px');
  });

  it('usa escala inteira 3x no tamanho grande', () => {
    montar('acenando', { tamanho: 'grande' });
    expect(sprite().style.getPropertyValue('--largura')).toBe('276px');
  });

  it('repete as animações de espera e percorre todos os quadros', () => {
    montar('buscando');
    expect(sprite().classList).toContain('repete');
    expect(sprite().style.getPropertyValue('--passos')).toBe('9');
  });

  it('toca uma vez e para no último quadro nas animações de momento', () => {
    montar('comemorando');
    expect(sprite().classList).not.toContain('repete');
    expect(sprite().style.getPropertyValue('--passos')).toBe('8');
    expect(sprite().style.getPropertyValue('--destino')).toBe('-1472px');
  });

  it('usa a tira da direção pedida quando ela existe', () => {
    montar('buscando', { direcao: 'west' });
    expect(sprite().style.getPropertyValue('--sprite')).toContain('buscando-west.png');
  });

  it('cai na frente quando o estado não tem a direção pedida, sem espelhar a arte', () => {
    montar('erro', { direcao: 'east' });
    expect(sprite().style.getPropertyValue('--sprite')).toContain('erro-south.png');
    expect(sprite().style.transform).toBe('');
  });
});
