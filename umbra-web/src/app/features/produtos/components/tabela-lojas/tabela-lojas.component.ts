import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { OfertaExibida } from './oferta-exibida';

@Component({
  selector: 'umbra-tabela-lojas',
  templateUrl: './tabela-lojas.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class TabelaLojasComponent {
  readonly ofertas = input.required<readonly OfertaExibida[]>();
  readonly mostrarDesconto = input(true);
}
