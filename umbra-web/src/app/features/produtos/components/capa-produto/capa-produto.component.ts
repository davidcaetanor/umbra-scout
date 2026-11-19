import { ChangeDetectionStrategy, Component, input, linkedSignal } from '@angular/core';
import { TipoExibido } from '../tipo-exibido';

@Component({
  selector: 'umbra-capa-produto',
  templateUrl: './capa-produto.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex items-center justify-center bg-superficie-elevada text-texto-secundario' },
})
export class CapaProdutoComponent {
  readonly imagem = input.required<string | null>();
  readonly tipo = input.required<TipoExibido>();

  protected readonly falhou = linkedSignal({ source: this.imagem, computation: () => false });
}
