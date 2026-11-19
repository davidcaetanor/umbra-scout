import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'umbra-paginacao',
  templateUrl: './paginacao.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class PaginacaoComponent {
  readonly pagina = input.required<number>();
  readonly totalDePaginas = input.required<number>();
  readonly irPara = output<number>();
}
