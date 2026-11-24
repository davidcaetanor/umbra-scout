import { lerReais } from './valor-em-reais';

export const ALVO_MAXIMO_CENTAVOS = 15_000_000;

export type LeituraDoAlvo =
  | { readonly tipo: 'valido'; readonly centavos: number }
  | { readonly tipo: 'vazio' }
  | { readonly tipo: 'invalido' }
  | { readonly tipo: 'zero' }
  | { readonly tipo: 'acima-do-maximo' };

const CENTAVOS_POR_REAL = 100;

export const MENSAGEM_DO_ALVO: Readonly<Record<Exclude<LeituraDoAlvo['tipo'], 'valido'>, string>> = {
  vazio: 'Informe o preço alvo.',
  invalido: 'Use só números, como 149,90 ou 149.90.',
  zero: 'O alvo precisa ser maior que zero.',
  'acima-do-maximo': 'O alvo vai até R$ 150.000,00.',
};

export function lerAlvo(texto: string): LeituraDoAlvo {
  const leitura = lerReais(texto);
  if (leitura.tipo !== 'valido') {
    return leitura;
  }
  if (leitura.centavos === 0) {
    return { tipo: 'zero' };
  }
  return leitura.centavos > ALVO_MAXIMO_CENTAVOS ? { tipo: 'acima-do-maximo' } : leitura;
}

export function textoDoAlvo(centavos: number): string {
  return (centavos / CENTAVOS_POR_REAL).toFixed(2).replace('.', ',');
}
