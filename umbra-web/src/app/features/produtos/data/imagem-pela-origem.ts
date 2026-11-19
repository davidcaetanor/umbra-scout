const ORIGEM_DO_ITAD = 'https://assets.isthereanydeal.com/';
const CAMINHO_PELA_NOSSA_ORIGEM = '/imagens/itad/';

export function imagemPelaOrigem(endereco: string | null | undefined): string | null {
  if (!endereco?.startsWith(ORIGEM_DO_ITAD)) {
    return null;
  }
  return CAMINHO_PELA_NOSSA_ORIGEM + endereco.slice(ORIGEM_DO_ITAD.length);
}
