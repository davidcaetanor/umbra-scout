import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CelulaDaLegenda } from './legenda-exibida';

@Component({
  selector: 'umbra-legenda-historico',
  templateUrl: './legenda-historico.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class LegendaHistoricoComponent {
  readonly celulas = input.required<readonly CelulaDaLegenda[]>();
}
