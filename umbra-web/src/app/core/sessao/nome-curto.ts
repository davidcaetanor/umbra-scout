const PARTICULAS = new Set(['da', 'das', 'de', 'do', 'dos', 'e', "d'"]);
const PALAVRAS_DO_NOME_CURTO = 2;

export function nomeCurto(nomeCompleto: string): string {
  const palavras = nomeCompleto.trim().split(/\s+/).filter((palavra) => palavra.length > 0);
  if (palavras.length === 0) {
    return '';
  }
  const [primeira, ...resto] = palavras;
  const segunda = resto.find((palavra) => !PARTICULAS.has(palavra.toLowerCase()));
  return [primeira, segunda]
    .filter((palavra): palavra is string => palavra !== undefined)
    .slice(0, PALAVRAS_DO_NOME_CURTO)
    .map(capitalizarSeUniforme)
    .join(' ');
}

function capitalizarSeUniforme(palavra: string): string {
  const uniforme = palavra === palavra.toLowerCase() || palavra === palavra.toUpperCase();
  if (!uniforme) {
    return palavra;
  }
  return palavra
    .toLowerCase()
    .split('-')
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join('-');
}
