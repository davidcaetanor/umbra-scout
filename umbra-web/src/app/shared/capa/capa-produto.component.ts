import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, signal } from '@angular/core';
import { CapaExibida } from './capa-exibida';
import { TipoExibido } from './tipo-exibido';

type FaseDaCapa = 'faixa' | 'reserva' | 'sem-imagem';

@Component({
  selector: 'umbra-capa-produto',
  templateUrl: './capa-produto.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'flex items-center justify-center overflow-hidden text-texto-secundario',
    '[class.bg-superficie-elevada]': '!emMoldura()',
    '[class.bg-moldura-foto]': 'emMoldura()',
  },
})
export class CapaProdutoComponent {
  readonly capa = input.required<CapaExibida | null>();
  readonly tipo = input.required<TipoExibido>();
  readonly tamanhos = input('120px');
  readonly prioritaria = input(false);

  protected readonly fase = linkedSignal<CapaExibida | null, FaseDaCapa>({
    source: this.capa,
    computation: (capa) => (capa === null ? 'sem-imagem' : 'faixa'),
  });
  protected readonly carregada = signal(false);
  protected readonly emMoldura = computed(() => this.capa()?.enquadramento === 'foto' && this.fase() !== 'sem-imagem');

  protected readonly endereco = computed(() => {
    const capa = this.capa();
    return this.fase() === 'reserva' ? capa?.reserva : capa?.endereco;
  });
  protected readonly alternativas = computed(() => (this.fase() === 'faixa' ? this.capa()?.alternativas || null : null));

  protected falhou(): void {
    this.carregada.set(false);
    this.fase.set(this.fase() === 'faixa' && this.capa()?.reserva ? 'reserva' : 'sem-imagem');
  }
}
