import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProdutoExibido } from '../produto-exibido';
import { CapaProdutoComponent } from '../capa-produto/capa-produto.component';
import { PrecoProdutoComponent } from '../preco-produto/preco-produto.component';

@Component({
  selector: 'umbra-card-produto',
  imports: [CapaProdutoComponent, PrecoProdutoComponent],
  templateUrl: './card-produto.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class CardProdutoComponent {
  readonly produto = input.required<ProdutoExibido>();
  readonly prioritaria = input(false);
}
