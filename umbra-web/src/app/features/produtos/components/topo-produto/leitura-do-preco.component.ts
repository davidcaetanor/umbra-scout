import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LeituraExibida } from './topo-exibido';

@Component({
  selector: 'umbra-leitura-do-preco',
  templateUrl: './leitura-do-preco.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class LeituraDoPrecoComponent {
  readonly leitura = input.required<LeituraExibida>();
}
