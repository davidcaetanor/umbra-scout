import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

export type TipoDeCampo = 'text' | 'email' | 'password';

@Component({
  selector: 'umbra-campo',
  imports: [ReactiveFormsModule],
  templateUrl: './campo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CampoComponent {
  readonly controle = input.required<FormControl<string>>();
  readonly id = input.required<string>();
  readonly rotulo = input.required<string>();
  readonly tipo = input<TipoDeCampo>('text');
  readonly autocomplete = input.required<string>();
  readonly dica = input<string | null>(null);
  readonly erro = input<string | null>(null);

  protected readonly senhaVisivel = signal(false);
  protected readonly ehSenha = computed(() => this.tipo() === 'password');
  protected readonly tipoEfetivo = computed(() => (this.ehSenha() && this.senhaVisivel() ? 'text' : this.tipo()));
  protected readonly idDaDica = computed(() => `${this.id()}-dica`);
  protected readonly idDoErro = computed(() => `${this.id()}-erro`);
  protected readonly descritoPor = computed(() => {
    const ids = [this.dica() ? this.idDaDica() : null, this.erro() ? this.idDoErro() : null];
    return ids.filter((valor) => valor !== null).join(' ') || null;
  });

  protected alternarSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }
}
