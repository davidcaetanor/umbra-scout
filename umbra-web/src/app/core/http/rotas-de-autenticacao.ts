const PREFIXO_DAS_ROTAS_DE_AUTENTICACAO = '/auth/';

export function ehRotaDeAutenticacao(caminho: string): boolean {
  return caminho.startsWith(PREFIXO_DAS_ROTAS_DE_AUTENTICACAO);
}
