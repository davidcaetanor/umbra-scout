export interface PrecoDoProduto {
  readonly atual: string;
  readonly original: string | null;
  readonly descontoPct: number | null;
  readonly loja: string;
}
