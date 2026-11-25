import { TestBed } from '@angular/core/testing';
import { AvisoComponent } from './aviso.component';
import { AvisoService, DURACAO_DO_AVISO_LONGO_MS, DURACAO_DO_AVISO_MS } from './aviso.service';

describe('AvisoComponent', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  function montar() {
    const fixture = TestBed.createComponent(AvisoComponent);
    fixture.detectChanges();
    return { fixture, servico: TestBed.inject(AvisoService), elemento: fixture.nativeElement as HTMLElement };
  }

  it('anuncia o aviso numa região viva com o retrato pedido', () => {
    const { fixture, servico, elemento } = montar();

    servico.mostrar('Monitoramento criado.');
    fixture.detectChanges();

    const regiao = elemento.querySelector('[role="status"]');
    expect(regiao?.getAttribute('aria-live')).toBe('polite');
    expect(regiao?.textContent).toContain('Monitoramento criado.');
    expect(elemento.querySelector('img')?.getAttribute('src')).toBe('/mascote/v1/retratos/feliz.png');
    expect(elemento.querySelector('img')?.getAttribute('width')).toBe('64');
  });

  it('some sozinho depois do prazo', () => {
    const { fixture, servico, elemento } = montar();

    servico.mostrar('Monitoramento criado.', 'tranquilo');
    vi.advanceTimersByTime(DURACAO_DO_AVISO_MS);
    fixture.detectChanges();

    expect(elemento.textContent).not.toContain('Monitoramento criado.');
  });

  it('fecha pelo botão e um aviso novo reinicia o prazo', () => {
    const { fixture, servico, elemento } = montar();

    servico.mostrar('Primeiro');
    vi.advanceTimersByTime(DURACAO_DO_AVISO_MS - 1);
    servico.mostrar('Segundo');
    vi.advanceTimersByTime(DURACAO_DO_AVISO_MS - 1);
    fixture.detectChanges();
    expect(elemento.textContent).toContain('Segundo');

    elemento.querySelector<HTMLButtonElement>('button[aria-label="Fechar aviso"]')?.click();
    fixture.detectChanges();
    expect(elemento.textContent).not.toContain('Segundo');
  });

  it('mostra o sprite e o detalhe num aviso longo', () => {
    const { fixture, servico, elemento } = montar();

    servico.mostrarLongo({ texto: 'Piada', detalhe: 'Monitoramento criado.', figura: { tipo: 'sprite', estado: 'velhinho' } });
    fixture.detectChanges();

    expect(elemento.querySelector('umbra-mascote')).not.toBeNull();
    expect(elemento.querySelector('img')).toBeNull();
    expect(elemento.textContent).toContain('Monitoramento criado.');
    vi.advanceTimersByTime(DURACAO_DO_AVISO_MS);
    fixture.detectChanges();
    expect(elemento.textContent).toContain('Piada');
    vi.advanceTimersByTime(DURACAO_DO_AVISO_LONGO_MS - DURACAO_DO_AVISO_MS);
    fixture.detectChanges();
    expect(elemento.textContent).not.toContain('Piada');
  });
});
