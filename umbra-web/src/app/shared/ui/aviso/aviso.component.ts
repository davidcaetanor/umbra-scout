import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AvisoService } from './aviso.service';

@Component({
  selector: 'umbra-aviso',
  templateUrl: './aviso.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'pointer-events-none fixed inset-x-4 bottom-24 z-30 flex justify-center tablet:inset-x-auto tablet:end-6 tablet:bottom-6',
  },
})
export class AvisoComponent {
  protected readonly avisos = inject(AvisoService);
}
