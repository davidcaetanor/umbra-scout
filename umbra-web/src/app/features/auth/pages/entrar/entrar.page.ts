import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';

@Component({
  selector: 'umbra-entrar-page',
  imports: [MascoteComponent],
  templateUrl: './entrar.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntrarPage {}
