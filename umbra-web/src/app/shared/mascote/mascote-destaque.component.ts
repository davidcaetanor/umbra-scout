import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { PASTA_DO_DESTAQUE } from './caminhos-da-arte';

export type LadoDaTela = 'esquerda' | 'direita';

const LADO_DA_ARTE = 256;
const SOBRA_ABAIXO_DOS_PES = 11;

const ARTE_POR_LADO: Readonly<Record<LadoDaTela, string>> = {
  esquerda: `${PASTA_DO_DESTAQUE}/umbra-destaque-256.png`,
  direita: `${PASTA_DO_DESTAQUE}/umbra-destaque-espelhado-256.png`,
};

@Component({
  selector: 'umbra-mascote-destaque',
  templateUrl: './mascote-destaque.component.html',
  styleUrl: './mascote-destaque.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
})
export class MascoteDestaqueComponent {
  readonly lado = input<LadoDaTela>('esquerda');

  protected readonly arte = computed(() => ARTE_POR_LADO[this.lado()]);
  protected readonly lado256 = LADO_DA_ARTE;
  protected readonly alturaVisivel = LADO_DA_ARTE - SOBRA_ABAIXO_DOS_PES;
}
