const FORMATO = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function reais(centavos: number): string {
  return FORMATO.format(centavos / 100).replace(/\s/u, ' ');
}

const ESPACO_INSEPARAVEL = '\u00a0';

export function reaisSemQuebra(centavos: number): string {
  return reais(centavos).replace(' ', ESPACO_INSEPARAVEL);
}
