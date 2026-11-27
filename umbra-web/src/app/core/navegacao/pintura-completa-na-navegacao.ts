import { ApplicationRef, DestroyRef, EnvironmentProviders, inject, provideEnvironmentInitializer } from '@angular/core';
import { ActivationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

export function providePinturaCompletaNaNavegacao(): EnvironmentProviders {
  return provideEnvironmentInitializer(() => {
    const aplicacao = inject(ApplicationRef);
    const assinatura = inject(Router)
      .events.pipe(filter((evento) => evento instanceof ActivationEnd))
      .subscribe(() => queueMicrotask(() => aplicacao.tick()));
    inject(DestroyRef).onDestroy(() => assinatura.unsubscribe());
  });
}
