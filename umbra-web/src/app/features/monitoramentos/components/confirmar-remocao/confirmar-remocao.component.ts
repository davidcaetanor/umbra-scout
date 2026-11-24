import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, input, output, viewChild } from '@angular/core';
import { reais } from '../../../../shared/formatacao/reais';

export interface RemocaoPedida {
  readonly nome: string;
  readonly precoAlvoCentavos: number;
}

@Component({
  selector: 'umbra-confirmar-remocao',
  templateUrl: './confirmar-remocao.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmarRemocaoComponent {
  readonly pedido = input<RemocaoPedida | null>(null);
  readonly removendo = input(false);

  readonly confirmar = output<void>();
  readonly cancelar = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly botaoCancelar = viewChild.required<ElementRef<HTMLButtonElement>>('botaoCancelar');

  protected readonly alvo = computed(() => {
    const pedido = this.pedido();
    return pedido === null ? '' : reais(pedido.precoAlvoCentavos);
  });

  constructor() {
    effect(() => {
      const dialogo = this.dialogo().nativeElement;
      if (this.pedido() === null) {
        if (dialogo.open) {
          dialogo.close();
        }
        return;
      }
      if (!dialogo.open) {
        dialogo.showModal();
        this.botaoCancelar().nativeElement.focus();
      }
    });
  }

  protected aoCancelarPeloTeclado(evento: Event): void {
    evento.preventDefault();
    this.pedirCancelamento();
  }

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
