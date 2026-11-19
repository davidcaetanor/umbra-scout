import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PrecoExibido } from '../preco-exibido';

@Component({
  selector: 'umbra-preco-produto',
  templateUrl: './preco-produto.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class PrecoProdutoComponent {
  readonly preco = input.required<PrecoExibido | null>();
}
