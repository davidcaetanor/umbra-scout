import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TipoDeAviso = 'informacao' | 'erro';

@Component({
  selector: 'umbra-aviso',
  templateUrl: './aviso.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[attr.role]': "tipo() === 'erro' ? 'alert' : 'status'", class: 'block' },
})
export class AvisoComponent {
  readonly tipo = input<TipoDeAviso>('informacao');
}
