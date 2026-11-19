export const DESTINO_PADRAO = '/catalogo';
export const ROTA_DE_ENTRADA = '/entrar';

const ROTAS_DE_ENTRADA = ['/entrar', '/cadastro', '/login'];

export function destinoInterno(valor: unknown): string {
  if (typeof valor !== 'string' || !valor.startsWith('/') || valor.startsWith('//') || valor.includes('\\')) {
    return DESTINO_PADRAO;
  }
  const caminho = valor.split(/[?#]/)[0];
  const levaDeVoltaAEntrada = ROTAS_DE_ENTRADA.some((rota) => caminho === rota || caminho.startsWith(`${rota}/`));
  return levaDeVoltaAEntrada ? DESTINO_PADRAO : valor;
}
