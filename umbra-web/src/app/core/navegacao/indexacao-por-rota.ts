import { DestroyRef, EnvironmentProviders, inject, provideEnvironmentInitializer } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

export const SEM_INDEXACAO = { indexavel: false } as const;

const SELETOR_ROBOTS = 'name="robots"';

export function provideIndexacaoPorRota(): EnvironmentProviders {
  return provideEnvironmentInitializer(() => {
    const router = inject(Router);
    const meta = inject(Meta);
    const assinatura = router.events.pipe(filter((evento) => evento instanceof NavigationEnd)).subscribe(() => {
      if (indexavel(router.routerState.snapshot.root)) {
        meta.removeTag(SELETOR_ROBOTS);
      } else {
        meta.updateTag({ name: 'robots', content: 'noindex' });
      }
    });
    inject(DestroyRef).onDestroy(() => assinatura.unsubscribe());
  });
}

function indexavel(rota: ActivatedRouteSnapshot | null): boolean {
  for (let atual = rota; atual; atual = atual.firstChild) {
    if (atual.data['indexavel'] === false) {
      return false;
    }
  }
  return true;
}
