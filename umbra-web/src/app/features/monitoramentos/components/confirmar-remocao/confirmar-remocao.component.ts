import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { reais } from '../../../../shared/formatacao/reais';
import { DialogoComponent } from '../../../../shared/ui/dialogo/dialogo.component';

export interface RemocaoPedida {
  readonly nome: string;
  readonly precoAlvoCentavos: number;
}

@Component({
  selector: 'umbra-confirmar-remocao',
  imports: [DialogoComponent],
  templateUrl: './confirmar-remocao.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmarRemocaoComponent {
  readonly pedido = input<RemocaoPedida | null>(null);
  readonly removendo = input(false);

  readonly confirmar = output<void>();
  readonly cancelar = output<void>();

  protected readonly aberto = computed(() => this.pedido() !== null);
  protected readonly alvo = computed(() => {
    const pedido = this.pedido();
    return pedido === null ? '' : reais(pedido.precoAlvoCentavos);
  });

  protected pedirCancelamento(): void {
    if (!this.removendo()) {
      this.cancelar.emit();
    }
  }

  protected pedirConfirmacao(): void {
    if (!this.removendo()) {
      this.confirmar.emit();
    }
  }
}
