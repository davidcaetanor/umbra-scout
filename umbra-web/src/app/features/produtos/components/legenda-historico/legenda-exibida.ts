export type MarcaDaLegenda = 'hoje' | 'linha-1' | 'linha-2' | 'minimo' | 'alvo';

export interface CelulaDaLegenda {
  readonly marca: MarcaDaLegenda;
  readonly titulo: string;
  readonly valor: string | null;
  readonly detalhe: string | null;
}
