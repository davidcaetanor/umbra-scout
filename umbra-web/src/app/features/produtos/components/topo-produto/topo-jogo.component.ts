import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CapaProdutoComponent } from '../capa-produto/capa-produto.component';
import { LeituraDoPrecoComponent } from './leitura-do-preco.component';
import { TopoExibido } from './topo-exibido';

@Component({
  selector: 'umbra-topo-jogo',
  imports: [CapaProdutoComponent, LeituraDoPrecoComponent],
  templateUrl: './topo-jogo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col gap-3' },
})
export class TopoJogoComponent {
  readonly topo = input.required<TopoExibido>();
}
