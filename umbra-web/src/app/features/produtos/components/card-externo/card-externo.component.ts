import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProdutoExternoExibido } from '../produto-externo-exibido';
import { CapaProdutoComponent } from '../capa-produto/capa-produto.component';
import { PrecoProdutoComponent } from '../preco-produto/preco-produto.component';

@Component({
  selector: 'umbra-card-externo',
  imports: [CapaProdutoComponent, PrecoProdutoComponent],
  templateUrl: './card-externo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class CardExternoComponent {
  readonly produto = input.required<ProdutoExternoExibido>();
}
