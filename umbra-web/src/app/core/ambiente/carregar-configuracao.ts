import { ConfiguracaoAmbiente } from './configuracao-ambiente';
import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';
import { lerConfiguracaoEmbutida } from './ler-configuracao-embutida';
import { validarConfiguracao } from './validar-configuracao';

const ENDERECO_DA_CONFIGURACAO = '/config.json';

export async function carregarConfiguracao(
  documento: Document = document,
  buscar: typeof fetch = fetch,
): Promise<ConfiguracaoAmbiente> {
  return lerConfiguracaoEmbutida(documento) ?? buscarConfiguracao(buscar);
}

async function buscarConfiguracao(buscar: typeof fetch): Promise<ConfiguracaoAmbiente> {
  const resposta = await buscar(ENDERECO_DA_CONFIGURACAO, { cache: 'no-store' });
  if (!resposta.ok) {
    throw new ConfiguracaoInvalidaError(`resposta ${resposta.status}`);
  }
  return validarConfiguracao(await resposta.json());
}
