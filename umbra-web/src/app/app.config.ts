import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { ConfiguracaoAmbiente, provideConfiguracaoAmbiente } from './core/ambiente';
import { routes } from './app.routes';

export function criarAppConfig(configuracao: ConfiguracaoAmbiente): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideConfiguracaoAmbiente(configuracao),
      provideHttpClient(withFetch()),
      provideRouter(routes, withComponentInputBinding()),
    ],
  };
}
