import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MascoteDestaqueComponent } from '../../../../shared/mascote/mascote-destaque.component';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';

@Component({
  selector: 'umbra-meus-monitoramentos-page',
  imports: [MascoteComponent, MascoteDestaqueComponent],
  templateUrl: './meus-monitoramentos.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeusMonitoramentosPage {}
