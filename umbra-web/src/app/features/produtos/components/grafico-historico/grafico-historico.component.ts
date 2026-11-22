import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterRenderEffect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import { Chart, LineController, LineElement, LinearScale, PointElement, Tooltip } from 'chart.js';
import { configuracaoDoGrafico } from './configuracao-do-grafico';
import { coresDoTema } from './cores-do-tema';
import { GraficoExibido, PontoExibido } from './grafico-exibido';

Chart.register(LineController, LineElement, PointElement, LinearScale, Tooltip);

@Component({
  selector: 'umbra-grafico-historico',
  templateUrl: './grafico-historico.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class GraficoHistoricoComponent {
  readonly grafico = input.required<GraficoExibido>();

  private readonly tela = viewChild.required<ElementRef<HTMLCanvasElement>>('tela');
  private readonly paleta = viewChild.required<ElementRef<HTMLElement>>('paleta');
  private instancia: Chart<'line', PontoExibido[]> | null = null;

  constructor() {
    afterRenderEffect(() => {
      const configuracao = configuracaoDoGrafico(this.grafico(), coresDoTema(this.paleta().nativeElement));
      this.instancia?.destroy();
      this.instancia = new Chart(this.tela().nativeElement, configuracao);
    });
    inject(DestroyRef).onDestroy(() => this.instancia?.destroy());
  }
}
