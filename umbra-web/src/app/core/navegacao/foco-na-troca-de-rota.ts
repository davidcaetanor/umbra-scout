import { DOCUMENT } from '@angular/common';
import { DestroyRef, EnvironmentProviders, Injector, afterNextRender, inject, provideEnvironmentInitializer } from '@angular/core';
import { NavigationEnd, Router, UrlTree } from '@angular/router';
import { filter, map, pairwise } from 'rxjs';

export const ID_CONTEUDO_PRINCIPAL = 'conteudo';

export function provideFocoNaTrocaDeRota(): EnvironmentProviders {
  return provideEnvironmentInitializer(() => {
    const router = inject(Router);
    const documento = inject(DOCUMENT);
    const injector = inject(Injector);
    const assinatura = router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        map((evento) => router.parseUrl(evento.urlAfterRedirects)),
        pairwise(),
        filter(([anterior, atual]) => atual.fragment !== null || caminhoDe(anterior) !== caminhoDe(atual)),
      )
      .subscribe(([, atual]) => afterNextRender(() => focar(documento, atual.fragment), { injector }));
    inject(DestroyRef).onDestroy(() => assinatura.unsubscribe());
  });
}

function caminhoDe(url: UrlTree): string {
  return url.root.children['primary']?.toString() ?? '';
}

function focar(documento: Document, fragmento: string | null): void {
  const indicado = fragmento === null ? null : documento.getElementById(fragmento);
  const alvo =
    indicado ??
    documento.querySelector<HTMLElement>(`#${ID_CONTEUDO_PRINCIPAL} h1`) ??
    documento.getElementById(ID_CONTEUDO_PRINCIPAL);
  if (!alvo) {
    return;
  }
  if (indicado === null) {
    alvo.setAttribute('tabindex', '-1');
  }
  alvo.focus({ preventScroll: false });
}
