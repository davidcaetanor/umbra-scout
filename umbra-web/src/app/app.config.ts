import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { TitleStrategy, provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { ConfiguracaoAmbiente, provideConfiguracaoAmbiente } from './core/ambiente';
import { autenticacaoInterceptor, erroInterceptor } from './core/http';
import { provideFocoNaTrocaDeRota } from './core/navegacao/foco-na-troca-de-rota';
import { provideIndexacaoPorRota } from './core/navegacao/indexacao-por-rota';
import { TituloComMarcaStrategy } from './core/navegacao/titulo-com-marca.strategy';
import { provideSessao } from './core/sessao';
import { routes } from './app.routes';

export function criarAppConfig(configuracao: ConfiguracaoAmbiente): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideConfiguracaoAmbiente(configuracao),
      provideHttpClient(withFetch(), withInterceptors([erroInterceptor, autenticacaoInterceptor])),
      provideSessao(),
      provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
      { provide: TitleStrategy, useClass: TituloComMarcaStrategy },
      provideFocoNaTrocaDeRota(),
      provideIndexacaoPorRota(),
    ],
  };
}
