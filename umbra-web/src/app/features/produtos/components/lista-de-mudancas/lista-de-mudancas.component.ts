import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { MudancaExibida } from './mudanca-exibida';
import { resumoDasMudancas } from './resumo-das-mudancas';

@Component({
  selector: 'umbra-lista-de-mudancas',
  templateUrl: './lista-de-mudancas.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col items-start gap-2' },
})
export class ListaDeMudancasComponent {
  readonly mudancas = input.required<readonly MudancaExibida[]>();
  protected readonly separador = ': ';
  protected readonly expandida = signal(false);
  protected readonly resumo = computed(() => resumoDasMudancas(this.mudancas()));
  protected readonly visiveis = computed(() => (this.expandida() ? this.mudancas() : this.resumo()));
  protected readonly temMais = computed(() => this.mudancas().length > this.resumo().length);

  protected alternar(): void {
    this.expandida.update((atual) => !atual);
  }
}
