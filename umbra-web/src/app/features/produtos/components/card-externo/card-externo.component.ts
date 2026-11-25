import { ChangeDetectionStrategy, Component, ElementRef, computed, input, output, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CapaProdutoComponent } from '../../../../shared/capa/capa-produto.component';
import { PrecoProdutoComponent } from '../preco-produto/preco-produto.component';
import { MonitoramentoDoExterno, ProdutoExternoExibido } from '../produto-externo-exibido';

@Component({
  selector: 'umbra-card-externo',
  imports: [CapaProdutoComponent, PrecoProdutoComponent, RouterLink],
  templateUrl: './card-externo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class CardExternoComponent {
  readonly produto = input.required<ProdutoExternoExibido>();
  readonly monitoramento = input<MonitoramentoDoExterno | null>(null);

  readonly monitorar = output<void>();

  protected readonly monitorando = computed(() => {
    const monitoramento = this.monitoramento();
    return monitoramento?.tipo === 'monitorando' ? monitoramento : null;
  });
  protected readonly jaMonitorado = computed(() => this.monitoramento()?.tipo === 'ja-monitorado');

  private readonly botaoMonitorar = viewChild<ElementRef<HTMLButtonElement>>('botaoMonitorar');
  private readonly linkDoMonitoramento = viewChild<ElementRef<HTMLAnchorElement>>('linkDoMonitoramento');

  focarMonitorar(): void {
    this.botaoMonitorar()?.nativeElement.focus();
  }

  focarMonitoramento(): void {
    this.linkDoMonitoramento()?.nativeElement.focus();
  }
}
