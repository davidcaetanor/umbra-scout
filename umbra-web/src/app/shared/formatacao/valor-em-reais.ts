export type LeituraDeReais =
  | { readonly tipo: 'valido'; readonly centavos: number }
  | { readonly tipo: 'vazio' }
  | { readonly tipo: 'invalido' };

const CENTAVOS_POR_REAL = 100;
const SO_NUMEROS_E_SEPARADORES = /^[\d.,]+$/;
const INTEIRO_SIMPLES = /^\d+$/;
const DECIMAIS = /^\d{1,2}$/;
const MAXIMO_SEGURO_EM_CENTAVOS = 99_999_999_999;

export function lerReais(texto: string): LeituraDeReais {
  const limpo = texto.trim().replace(/^R\$\s*/i, '');
  if (limpo === '') {
    return { tipo: 'vazio' };
  }
  if (!SO_NUMEROS_E_SEPARADORES.test(limpo)) {
    return { tipo: 'invalido' };
  }
  const centavos = centavosDe(limpo);
  return centavos === null || centavos > MAXIMO_SEGURO_EM_CENTAVOS ? { tipo: 'invalido' } : { tipo: 'valido', centavos };
}

export function reaisParaCampo(centavos: number): string {
  return centavos % CENTAVOS_POR_REAL === 0
    ? String(centavos / CENTAVOS_POR_REAL)
    : (centavos / CENTAVOS_POR_REAL).toFixed(2).replace('.', ',');
}

function centavosDe(texto: string): number | null {
  const ultimoSeparador = Math.max(texto.lastIndexOf(','), texto.lastIndexOf('.'));
  const depois = ultimoSeparador < 0 ? '' : texto.slice(ultimoSeparador + 1);
  const temDecimais = ultimoSeparador >= 0 && DECIMAIS.test(depois);
  const parteInteira = temDecimais ? texto.slice(0, ultimoSeparador) : texto;
  const separadorDecimal = temDecimais ? texto[ultimoSeparador] : null;
  const reais = inteiroDe(parteInteira, separadorDecimal);
  if (reais === null) {
    return null;
  }
  const fracao = temDecimais ? Number(depois.padEnd(2, '0')) : 0;
  return reais * CENTAVOS_POR_REAL + fracao;
}

function inteiroDe(texto: string, separadorDecimal: string | null): number | null {
  if (INTEIRO_SIMPLES.test(texto)) {
    return Number(texto);
  }
  const separadorDeMilhar = separadorDecimal === ',' ? '.' : separadorDecimal === '.' ? ',' : texto.match(/[.,]/)?.[0];
  if (separadorDeMilhar === undefined) {
    return null;
  }
  const grupos = texto.split(separadorDeMilhar);
  const agrupado = /^\d{1,3}$/.test(grupos[0]) && grupos.slice(1).every((grupo) => /^\d{3}$/.test(grupo));
  return grupos.length > 1 && agrupado ? Number(grupos.join('')) : null;
}
