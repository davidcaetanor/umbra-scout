import { CapaDoProduto } from './capa-do-produto';

const ORIGEM_DO_ITAD = 'https://assets.isthereanydeal.com/';
const CAMINHO_PELA_NOSSA_ORIGEM = '/imagens/itad/';
const ORIGEM_DA_KABUM = 'https://images.kabum.com.br/';
const CAMINHO_DA_KABUM_PELA_NOSSA_ORIGEM = '/imagens/kabum/';
const ARQUIVO_DA_CAPA_EM_PE = /\/boxart\.(?:jpg|png)(?=\?|$)/;
const LARGURAS_DA_FAIXA: readonly number[] = [300, 400];
export const LARGURAS_DA_FAIXA_EM_DESTAQUE: readonly number[] = [400, 600];

export function capaPelaOrigem(
  endereco: string | null | undefined,
  larguras: readonly number[] = LARGURAS_DA_FAIXA,
): CapaDoProduto | null {
  if (endereco?.startsWith(ORIGEM_DA_KABUM)) {
    return {
      endereco: CAMINHO_DA_KABUM_PELA_NOSSA_ORIGEM + endereco.slice(ORIGEM_DA_KABUM.length),
      alternativas: '',
      reserva: null,
      enquadramento: 'foto',
    };
  }
  if (!endereco?.startsWith(ORIGEM_DO_ITAD)) {
    return null;
  }
  const local = CAMINHO_PELA_NOSSA_ORIGEM + endereco.slice(ORIGEM_DO_ITAD.length);
  if (!ARQUIVO_DA_CAPA_EM_PE.test(local)) {
    return { endereco: local, alternativas: '', reserva: null, enquadramento: 'faixa' };
  }
  const faixa = (largura: number) => local.replace(ARQUIVO_DA_CAPA_EM_PE, `/banner${largura}.jpg`);
  return {
    endereco: faixa(larguras[0]),
    alternativas: larguras.map((largura) => `${faixa(largura)} ${largura}w`).join(', '),
    reserva: local,
    enquadramento: 'faixa',
  };
}
