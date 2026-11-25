import { ConfiguracaoAmbiente } from './configuracao-ambiente';
import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';
import { validarConfiguracao } from './validar-configuracao';

const ID_DO_BLOCO = 'configuracao';

export function lerConfiguracaoEmbutida(documento: Document): ConfiguracaoAmbiente | null {
  const bloco = documento.getElementById(ID_DO_BLOCO);
  if (bloco === null) {
    return null;
  }
  return validarConfiguracao(interpretar(bloco.textContent ?? ''));
}

function interpretar(conteudo: string): unknown {
  try {
    return JSON.parse(conteudo);
  } catch {
    throw new ConfiguracaoInvalidaError('bloco de configuração embutido não é JSON');
  }
}
