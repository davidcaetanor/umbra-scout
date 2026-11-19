import { ConfiguracaoAmbiente } from './configuracao-ambiente';
import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';

const PROTOCOLOS_ACEITOS = new Set(['http:', 'https:']);

export function validarConfiguracao(bruta: unknown): ConfiguracaoAmbiente {
  if (typeof bruta !== 'object' || bruta === null) {
    throw new ConfiguracaoInvalidaError('esperado um objeto');
  }
  const { apiUrl, loginGoogle } = bruta as Record<string, unknown>;
  return { apiUrl: validarApiUrl(apiUrl), loginGoogle: validarLoginGoogle(loginGoogle) };
}

function validarApiUrl(valor: unknown): string {
  if (typeof valor !== 'string' || !URL.canParse(valor)) {
    throw new ConfiguracaoInvalidaError('apiUrl precisa ser um endereço absoluto');
  }
  const url = new URL(valor);
  if (!PROTOCOLOS_ACEITOS.has(url.protocol)) {
    throw new ConfiguracaoInvalidaError('apiUrl precisa usar http ou https');
  }
  if (url.pathname !== '/' || url.search || url.hash) {
    throw new ConfiguracaoInvalidaError('apiUrl é só a origem, sem caminho, consulta ou fragmento');
  }
  return url.origin;
}

function validarLoginGoogle(valor: unknown): boolean {
  if (typeof valor !== 'boolean') {
    throw new ConfiguracaoInvalidaError('loginGoogle precisa ser true ou false');
  }
  return valor;
}
