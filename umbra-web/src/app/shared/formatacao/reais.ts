const FORMATO = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function reais(centavos: number): string {
  return FORMATO.format(centavos / 100).replace(/\s/u, ' ');
}
