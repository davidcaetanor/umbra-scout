import { ChangeDetectionStrategy, Component, ElementRef, effect, input, output, viewChild } from '@angular/core';

export type PapelDoDialogo = 'dialog' | 'alertdialog';

const SELETOR_DO_FOCO_INICIAL = '[data-foco-inicial]';

@Component({
  selector: 'umbra-dialogo',
  templateUrl: './dialogo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoComponent {
  readonly aberto = input(false);
  readonly papel = input<PapelDoDialogo>('dialog');
  readonly rotuladoPor = input.required<string>();
  readonly descritoPor = input<string | null>(null);
  readonly ocupado = input(false);

  readonly fechar = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  constructor() {
    effect(() => {
      const dialogo = this.dialogo().nativeElement;
      if (!this.aberto()) {
        if (dialogo.open) {
          dialogo.close();
        }
        return;
      }
      if (!dialogo.open) {
        dialogo.showModal();
        focarInicio(dialogo);
      }
    });
  }

  protected aoCancelarPeloTeclado(evento: Event): void {
    evento.preventDefault();
    if (!this.ocupado()) {
      this.fechar.emit();
    }
  }
}

function focarInicio(dialogo: HTMLDialogElement): void {
  const alvo = dialogo.querySelector<HTMLElement>(SELETOR_DO_FOCO_INICIAL);
  alvo?.focus();
  if (alvo instanceof HTMLInputElement) {
    alvo.select();
  }
}
