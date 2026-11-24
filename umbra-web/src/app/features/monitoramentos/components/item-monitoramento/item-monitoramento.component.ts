import { ChangeDetectionStrategy, Component, ElementRef, computed, input, output, viewChild } from '@angular/core';
import { CapaProdutoComponent } from '../../../../shared/capa/capa-produto.component';
import { reais } from '../../../../shared/formatacao/reais';
import { alvoAConfirmar, avisoDoPreco, distancia, preenchimento } from '../leitura-da-lista';
import { MonitoramentoExibido } from '../monitoramento-exibido';

@Component({
  selector: 'umbra-item-monitoramento',
  imports: [CapaProdutoComponent],
  templateUrl: './item-monitoramento.component.html',
  styleUrl: './item-monitoramento.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemMonitoramentoComponent {
  readonly item = input.required<MonitoramentoExibido>();
  readonly ocupado = input(false);

  readonly alternarAtivo = output<void>();
  readonly remover = output<void>();
  readonly alterarAlvo = output<void>();

  private readonly titulo = viewChild.required<ElementRef<HTMLElement>>('titulo');
  private readonly botaoRemover = viewChild.required<ElementRef<HTMLButtonElement>>('botaoRemover');
  private readonly botaoAlterar = viewChild.required<ElementRef<HTMLButtonElement>>('botaoAlterar');

  protected readonly idDoTitulo = computed(() => `monitoramento-${this.item().id}`);
  protected readonly preco = computed(() => {
    const melhor = this.item().melhorPreco;
    if (melhor === null) {
      return null;
    }
    return { valor: melhor.gratuito ? 'Gratuito' : reais(melhor.valorCentavos), loja: melhor.loja };
  });
  protected readonly alvo = computed(() => reais(this.item().precoAlvoCentavos));
  protected readonly preenchimento = computed(() => preenchimento(this.item()));
  protected readonly aConfirmar = computed(() => alvoAConfirmar(this.item()));
  protected readonly distancia = computed(() => distancia(this.item()));
  protected readonly aviso = computed(() => avisoDoPreco(this.item()));
  protected readonly acaoDoAtivo = computed(() => (this.item().ativo ? 'Pausar' : 'Reativar'));

  focarTitulo(): void {
    this.titulo().nativeElement.focus();
  }

  focarRemocao(): void {
    this.botaoRemover().nativeElement.focus();
  }

  focarAlteracao(): void {
    this.botaoAlterar().nativeElement.focus();
  }

  protected pedirAlteracao(): void {
    if (!this.ocupado()) {
      this.alterarAlvo.emit();
    }
  }

  protected pedirAlternancia(): void {
    if (!this.ocupado()) {
      this.alternarAtivo.emit();
    }
  }

  protected pedirRemocao(): void {
    if (!this.ocupado()) {
      this.remover.emit();
    }
  }
}
