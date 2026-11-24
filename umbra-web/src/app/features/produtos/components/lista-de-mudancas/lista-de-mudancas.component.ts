import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MudancaExibida } from './mudanca-exibida';

@Component({
  selector: 'umbra-lista-de-mudancas',
  templateUrl: './lista-de-mudancas.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class ListaDeMudancasComponent {
  readonly mudancas = input.required<readonly MudancaExibida[]>();
  protected readonly separador = ': ';
}
