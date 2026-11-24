import { PrecoExibido } from '../preco-exibido';

export interface SituacaoExibida {
  readonly texto: string;
  readonly aviso: boolean;
}

export interface OfertaExibida {
  readonly loja: string;
  readonly preco: PrecoExibido;
  readonly endereco: string;
  readonly maisBarata: boolean;
  readonly situacao: SituacaoExibida;
}
