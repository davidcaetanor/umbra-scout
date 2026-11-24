export interface CoresDoTema {
  readonly linha1: string;
  readonly linha2: string;
  readonly hoje: string;
  readonly minimo: string;
  readonly apoio: string;
  readonly alvo: string;
  readonly grade: string;
  readonly rotulo: string;
  readonly fonte: string;
}

export function coresDoTema(paleta: HTMLElement): CoresDoTema {
  const cor = (papel: string): string => {
    const amostra = paleta.querySelector<HTMLElement>(`[data-papel="${papel}"]`);
    return amostra ? getComputedStyle(amostra).color : '';
  };
  return {
    linha1: cor('linha-1'),
    linha2: cor('linha-2'),
    hoje: cor('hoje'),
    minimo: cor('minimo'),
    apoio: cor('apoio'),
    alvo: cor('alvo'),
    grade: cor('grade'),
    rotulo: cor('rotulo'),
    fonte: getComputedStyle(paleta).fontFamily,
  };
}
