import { MudancaExibida } from './mudanca-exibida';

export const MUDANCAS_NO_RESUMO = 3;

export function resumoDasMudancas(todas: readonly MudancaExibida[], limite = MUDANCAS_NO_RESUMO): MudancaExibida[] {
  const recentes = todas.slice(0, limite);
  const menor = todas.find((mudanca) => mudanca.menor);
  if (menor === undefined || recentes.includes(menor)) {
    return recentes;
  }
  const escolhidas = new Set([...todas.slice(0, limite - 1), menor]);
  return todas.filter((mudanca) => escolhidas.has(mudanca));
}
