import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export type ProvedorExterno = 'discord' | 'google';

const NOME_DO_PROVEDOR: Readonly<Record<ProvedorExterno, string>> = {
  discord: 'Discord',
  google: 'Google',
};

@Component({
  selector: 'umbra-botoes-login-externo',
  templateUrl: './botoes-login-externo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BotoesLoginExternoComponent {
  readonly provedores = input.required<readonly ProvedorExterno[]>();
  readonly verbo = input<'Entrar' | 'Continuar'>('Entrar');
  readonly saindo = input<ProvedorExterno | null>(null);

  readonly escolher = output<ProvedorExterno>();

  protected readonly nomes = NOME_DO_PROVEDOR;
}
