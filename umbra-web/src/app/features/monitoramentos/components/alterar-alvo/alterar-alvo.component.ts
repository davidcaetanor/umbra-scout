import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, input, output, signal, viewChild } from '@angular/core';
import { MENSAGEM_DO_ALVO, lerAlvo, textoDoAlvo } from '../../../../shared/formatacao/preco-alvo';
import { reais } from '../../../../shared/formatacao/reais';

export interface AlteracaoPedida {
  readonly nome: string;
  readonly precoAlvoCentavos: number;
  readonly melhorPrecoCentavos: number | null;
}

@Component({
  selector: 'umbra-alterar-alvo',
  templateUrl: './alterar-alvo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlterarAlvoComponent {
  readonly pedido = input<AlteracaoPedida | null>(null);
  readonly salvando = input(false);
  readonly erroDoServidor = input<string | null>(null);

  readonly salvar = output<number>();
  readonly cancelar = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly campo = viewChild.required<ElementRef<HTMLInputElement>>('campo');

  protected readonly texto = signal('');
  private readonly erroDoCampo = signal<string | null>(null);
  protected readonly erro = computed(() => this.erroDoCampo() ?? this.erroDoServidor());

  protected readonly alvoAtual = computed(() => {
    const pedido = this.pedido();
    return pedido === null ? '' : reais(pedido.precoAlvoCentavos);
  });
  protected readonly precoAtual = computed(() => {
    const centavos = this.pedido()?.melhorPrecoCentavos;
    return centavos == null ? null : reais(centavos);
  });

  constructor() {
    effect(() => {
      const dialogo = this.dialogo().nativeElement;
      const pedido = this.pedido();
      if (pedido === null) {
        if (dialogo.open) {
          dialogo.close();
        }
        return;
      }
      if (!dialogo.open) {
        this.texto.set(textoDoAlvo(pedido.precoAlvoCentavos));
        this.erroDoCampo.set(null);
        dialogo.showModal();
        const campo = this.campo().nativeElement;
        campo.value = this.texto();
        campo.focus();
        campo.select();
      }
    });
  }

  protected digitar(evento: Event): void {
    this.texto.set((evento.target as HTMLInputElement).value);
    this.erroDoCampo.set(null);
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();
    if (this.salvando()) {
      return;
    }
    const leitura = lerAlvo(this.texto());
    if (leitura.tipo !== 'valido') {
      this.erroDoCampo.set(MENSAGEM_DO_ALVO[leitura.tipo]);
      this.campo().nativeElement.focus();
      return;
    }
    this.salvar.emit(leitura.centavos);
  }

  protected aoCancelarPeloTeclado(evento: Event): void {
    evento.preventDefault();
    this.pedirCancelamento();
  }

  protected pedirCancelamento(): void {
    if (!this.salvando()) {
      this.cancelar.emit();
    }
  }
}
