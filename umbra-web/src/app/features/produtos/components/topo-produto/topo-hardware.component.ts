import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CapaProdutoComponent } from '../capa-produto/capa-produto.component';
import { LeituraDoPrecoComponent } from './leitura-do-preco.component';
import { TopoExibido } from './topo-exibido';

@Component({
  selector: 'umbra-topo-hardware',
  imports: [CapaProdutoComponent, LeituraDoPrecoComponent],
  templateUrl: './topo-hardware.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col gap-4' },
})
export class TopoHardwareComponent {
  readonly topo = input.required<TopoExibido>();
}
