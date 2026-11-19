import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export type LadoDaPonta = 'esquerda' | 'direita';

@Component({
  selector: 'umbra-balao-fala',
  templateUrl: './balao-fala.component.html',
  styleUrl: './balao-fala.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BalaoFalaComponent {
  readonly ponta = input<LadoDaPonta>('direita');
  readonly fechavel = input(true);
  readonly fechado = output<void>();
}
