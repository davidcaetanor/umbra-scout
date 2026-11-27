import { CapaDoProduto } from './capa-do-produto';
import { LARGURAS_DA_LISTA, LarguraDaCapa } from './larguras-da-capa';

interface OrigemDaImagem {
  readonly endereco: string;
  readonly caminhoLocal: string;
  readonly enquadramento: CapaDoProduto['enquadramento'];
  readonly arquivoPorLargura: (arquivo: string, largura: LarguraDaCapa) => string;
}

const CAPA_EM_PE_DO_ITAD = /\/boxart\.(?:jpg|png)(?=\?|$)/;
const FAIXA_DO_ITAD_POR_LARGURA: Readonly<Record<LarguraDaCapa, number>> = { 240: 300, 480: 600, 600: 600 };

const ORIGENS: readonly OrigemDaImagem[] = [
  {
    endereco: 'https://assets.isthereanydeal.com/',
    caminhoLocal: 'itad',
    enquadramento: 'faixa',
    arquivoPorLargura: faixaDoItad,
  },
  {
    endereco: 'https://images.kabum.com.br/',
    caminhoLocal: 'kabum',
    enquadramento: 'foto',
    arquivoPorLargura: (arquivo) => arquivo,
  },
];

export function capaPelaOrigem(
  endereco: string | null | undefined,
  larguras: readonly LarguraDaCapa[] = LARGURAS_DA_LISTA,
): CapaDoProduto | null {
  const origem = ORIGENS.find((candidata) => endereco?.startsWith(candidata.endereco));
  if (!endereco || !origem) {
    return null;
  }
  const arquivo = endereco.slice(origem.endereco.length);
  const variante = (largura: LarguraDaCapa) =>
    `/imagens/${largura}/${origem.caminhoLocal}/${origem.arquivoPorLargura(arquivo, largura)}`;
  return {
    endereco: variante(larguras[0]),
    alternativas: larguras.map((largura) => `${variante(largura)} ${largura}w`).join(', '),
    enquadramento: origem.enquadramento,
  };
}

function faixaDoItad(arquivo: string, largura: LarguraDaCapa): string {
  return arquivo.replace(CAPA_EM_PE_DO_ITAD, `/banner${FAIXA_DO_ITAD_POR_LARGURA[largura]}.jpg`);
}
