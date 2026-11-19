import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { TitleStrategy, provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { ConfiguracaoAmbiente, provideConfiguracaoAmbiente } from './core/ambiente';
import { provideFocoNaTrocaDeRota } from './core/navegacao/foco-na-troca-de-rota';
import { TituloComMarcaStrategy } from './core/navegacao/titulo-com-marca.strategy';
import { routes } from './app.routes';

export function criarAppConfig(configuracao: ConfiguracaoAmbiente): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideConfiguracaoAmbiente(configuracao),
      provideHttpClient(withFetch()),
      provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
      { provide: TitleStrategy, useClass: TituloComMarcaStrategy },
      provideFocoNaTrocaDeRota(),
    ],
  };
}
