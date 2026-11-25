import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal, untracked } from '@angular/core';
import { MENSAGEM_DO_ALVO, lerAlvo, textoDoAlvo } from '../../formatacao/preco-alvo';
import { DialogoComponent } from './dialogo.component';

export interface PedidoDeAlvo {
  readonly titulo: string;
  readonly apoio: string | null;
  readonly rotuloDoCampo: string;
  readonly alvoCentavos: number | null;
  readonly acao: string;
  readonly acaoEmAndamento: string;
  readonly focarAcao: boolean;
}

@Component({
  selector: 'umbra-dialogo-de-alvo',
  imports: [DialogoComponent],
  templateUrl: './dialogo-de-alvo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoDeAlvoComponent {
  readonly pedido = input<PedidoDeAlvo | null>(null);
  readonly enviando = input(false);
  readonly erroDoServidor = input<string | null>(null);

  readonly salvar = output<number>();
  readonly cancelar = output<void>();

  protected readonly texto = signal('');
  private readonly erroDoCampo = signal<string | null>(null);
  protected readonly erro = computed(() => this.erroDoCampo() ?? this.erroDoServidor());
  protected readonly aberto = computed(() => this.pedido() !== null);
  protected readonly focarAcao = computed(() => this.pedido()?.focarAcao ?? false);

  constructor() {
    effect(() => {
      const pedido = this.pedido();
      untracked(() => {
        this.erroDoCampo.set(null);
        this.texto.set(pedido?.alvoCentavos == null ? '' : textoDoAlvo(pedido.alvoCentavos));
      });
    });
  }

  protected digitar(evento: Event): void {
    this.texto.set((evento.target as HTMLInputElement).value);
    this.erroDoCampo.set(null);
  }

  protected enviar(evento: Event, campo: HTMLInputElement): void {
    evento.preventDefault();
    if (this.enviando()) {
      return;
    }
    const leitura = lerAlvo(this.texto());
    if (leitura.tipo !== 'valido') {
      this.erroDoCampo.set(MENSAGEM_DO_ALVO[leitura.tipo]);
      campo.focus();
      return;
    }
    this.salvar.emit(leitura.centavos);
  }

  protected pedirCancelamento(): void {
    if (!this.enviando()) {
      this.cancelar.emit();
    }
  }
}
