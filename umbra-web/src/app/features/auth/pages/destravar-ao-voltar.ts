import { DOCUMENT } from '@angular/common';
import { DestroyRef, WritableSignal, inject } from '@angular/core';

export function destravarAoVoltar<T>(saindoPara: WritableSignal<T | null>): void {
  const janela = inject(DOCUMENT).defaultView;
  if (janela === null) {
    return;
  }
  const aoMostrar = (evento: PageTransitionEvent): void => {
    if (evento.persisted) {
      saindoPara.set(null);
    }
  };
  janela.addEventListener('pageshow', aoMostrar);
  inject(DestroyRef).onDestroy(() => janela.removeEventListener('pageshow', aoMostrar));
}
