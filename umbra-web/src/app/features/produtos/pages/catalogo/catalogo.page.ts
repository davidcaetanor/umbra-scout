import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';

@Component({
  selector: 'umbra-catalogo-page',
  imports: [MascoteComponent],
  templateUrl: './catalogo.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogoPage {}
