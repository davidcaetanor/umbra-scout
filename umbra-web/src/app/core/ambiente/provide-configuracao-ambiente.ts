import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { ApiConfiguration } from '../../api/api-configuration';
import { ConfiguracaoAmbiente } from './configuracao-ambiente';
import { CONFIGURACAO_AMBIENTE } from './configuracao-ambiente.token';

export function provideConfiguracaoAmbiente(configuracao: ConfiguracaoAmbiente): EnvironmentProviders {
  const api = new ApiConfiguration();
  api.rootUrl = configuracao.apiUrl;
  return makeEnvironmentProviders([
    { provide: CONFIGURACAO_AMBIENTE, useValue: configuracao },
    { provide: ApiConfiguration, useValue: api },
  ]);
}
