import { CapaDoProduto } from './capa-do-produto';

const ORIGEM_DO_ITAD = 'https://assets.isthereanydeal.com/';
const CAMINHO_PELA_NOSSA_ORIGEM = '/imagens/itad/';
const ARQUIVO_DA_CAPA_EM_PE = /\/boxart\.jpg(?=\?|$)/;
const LARGURAS_DA_FAIXA = [300, 400] as const;

export function capaPelaOrigem(endereco: string | null | undefined): CapaDoProduto | null {
  if (!endereco?.startsWith(ORIGEM_DO_ITAD)) {
    return null;
  }
  const local = CAMINHO_PELA_NOSSA_ORIGEM + endereco.slice(ORIGEM_DO_ITAD.length);
  if (!ARQUIVO_DA_CAPA_EM_PE.test(local)) {
    return { endereco: local, alternativas: '', reserva: null };
  }
  const faixa = (largura: number) => local.replace(ARQUIVO_DA_CAPA_EM_PE, `/banner${largura}.jpg`);
  return {
    endereco: faixa(LARGURAS_DA_FAIXA[0]),
    alternativas: LARGURAS_DA_FAIXA.map((largura) => `${faixa(largura)} ${largura}w`).join(', '),
    reserva: local,
  };
}
