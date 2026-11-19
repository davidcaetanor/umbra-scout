import { DOCUMENT } from '@angular/common';
import { DestroyRef, EnvironmentProviders, afterNextRender, inject, provideEnvironmentInitializer, Injector } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, skip } from 'rxjs';

export const ID_CONTEUDO_PRINCIPAL = 'conteudo';

export function provideFocoNaTrocaDeRota(): EnvironmentProviders {
  return provideEnvironmentInitializer(() => {
    const router = inject(Router);
    const documento = inject(DOCUMENT);
    const injector = inject(Injector);
    const assinatura = router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        skip(1),
      )
      .subscribe(() => afterNextRender(() => focarTitulo(documento), { injector }));
    inject(DestroyRef).onDestroy(() => assinatura.unsubscribe());
  });
}

function focarTitulo(documento: Document): void {
  const alvo =
    documento.querySelector<HTMLElement>(`#${ID_CONTEUDO_PRINCIPAL} h1`) ??
    documento.getElementById(ID_CONTEUDO_PRINCIPAL);
  if (!alvo) {
    return;
  }
  alvo.setAttribute('tabindex', '-1');
  alvo.focus({ preventScroll: false });
}
