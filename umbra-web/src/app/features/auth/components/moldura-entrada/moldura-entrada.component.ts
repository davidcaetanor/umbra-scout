import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MascoteDestaqueComponent } from '../../../../shared/mascote/mascote-destaque.component';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';

@Component({
  selector: 'umbra-moldura-entrada',
  imports: [MascoteComponent, MascoteDestaqueComponent],
  templateUrl: './moldura-entrada.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MolduraEntradaComponent {}
