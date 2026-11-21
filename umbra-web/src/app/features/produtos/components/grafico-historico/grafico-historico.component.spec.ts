import { TestBed } from '@angular/core/testing';
import { Chart } from 'chart.js';
import { GraficoExibido } from './grafico-exibido';
import { GraficoHistoricoComponent } from './grafico-historico.component';

const GRAFICO: GraficoExibido = {
  inicio: 0,
  fim: 10,
  linhas: [{ rotulo: 'Melhor preço do dia', cor: 'historico-1', pontos: [{ x: 0, y: 100 }] }],
  minimo: null,
  hoje: null,
  alvo: null,
  resumo: 'Histórico de preço em 90 dias, hoje R$ 1,00.',
};

describe('GraficoHistoricoComponent', () => {
  afterEach(() => vi.restoreAllMocks());

  it('descreve o gráfico em texto, recria a cada mudança e destrói ao sair', async () => {
    const destruir = vi.spyOn(Chart.prototype, 'destroy');
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const fixture = TestBed.createComponent(GraficoHistoricoComponent);
    fixture.componentRef.setInput('grafico', GRAFICO);
    await fixture.whenStable();

    const tela = (fixture.nativeElement as HTMLElement).querySelector('canvas');
    expect(tela?.getAttribute('role')).toBe('img');
    expect(tela?.getAttribute('aria-label')).toBe(GRAFICO.resumo);

    fixture.componentRef.setInput('grafico', { ...GRAFICO, resumo: 'outro' });
    await fixture.whenStable();
    expect(destruir).toHaveBeenCalledTimes(1);

    fixture.destroy();
    expect(destruir).toHaveBeenCalledTimes(2);
  });
});
