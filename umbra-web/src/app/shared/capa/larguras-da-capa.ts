export const LARGURAS_DA_CAPA = [240, 480, 600] as const;

export type LarguraDaCapa = (typeof LARGURAS_DA_CAPA)[number];

export const LARGURAS_DA_LISTA: readonly LarguraDaCapa[] = [240, 480];
export const LARGURAS_DO_DESTAQUE: readonly LarguraDaCapa[] = LARGURAS_DA_CAPA;
