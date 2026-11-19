import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';

@Component({
  selector: 'umbra-meus-monitoramentos-page',
  imports: [MascoteComponent],
  templateUrl: './meus-monitoramentos.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeusMonitoramentosPage {}
