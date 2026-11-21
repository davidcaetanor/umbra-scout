const FORMATO_EM_REAIS = /^(\d{1,7})(?:[.,](\d{1,2}))?$/;
const CENTAVOS_POR_REAL = 100;

export function centavosDoAlvo(texto: string): number | null {
  const limpo = texto.trim().replace(/^R\$\s*/i, '');
  const partes = FORMATO_EM_REAIS.exec(limpo);
  if (partes === null) {
    return null;
  }
  const centavos = Number(partes[1]) * CENTAVOS_POR_REAL + Number((partes[2] ?? '0').padEnd(2, '0'));
  return centavos > 0 ? centavos : null;
}

export function textoDoAlvo(centavos: number): string {
  return (centavos / CENTAVOS_POR_REAL).toFixed(2).replace('.', ',');
}
