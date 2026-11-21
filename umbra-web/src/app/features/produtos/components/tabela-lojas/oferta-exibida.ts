import { PrecoExibido } from '../preco-exibido';

export interface OfertaExibida {
  readonly loja: string;
  readonly preco: PrecoExibido;
  readonly endereco: string;
  readonly maisBarata: boolean;
}
