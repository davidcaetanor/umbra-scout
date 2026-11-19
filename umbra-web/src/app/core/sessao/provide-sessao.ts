import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { SessaoService } from './sessao.service';

export function provideSessao(): EnvironmentProviders {
  return provideAppInitializer(() => {
    inject(SessaoService);
  });
}
