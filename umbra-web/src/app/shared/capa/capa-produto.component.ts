import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';
import { CapaExibida } from './capa-exibida';
import { TipoExibido } from './tipo-exibido';

type FaseDaCapa = 'imagem' | 'sem-imagem';

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
    computation: (capa) => (capa === null ? 'sem-imagem' : 'imagem'),
  });
  protected readonly emMoldura = computed(() => this.capa()?.enquadramento === 'foto' && this.fase() !== 'sem-imagem');

  protected readonly endereco = computed(() => this.capa()?.endereco);
  protected readonly alternativas = computed(() => this.capa()?.alternativas || null);

  protected falhou(): void {
    this.fase.set('sem-imagem');
  }
}
