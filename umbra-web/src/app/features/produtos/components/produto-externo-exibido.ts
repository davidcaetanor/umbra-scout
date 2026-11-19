import { PrecoExibido } from './preco-exibido';

export interface ProdutoExternoExibido {
  readonly nome: string;
  readonly imagem: string | null;
  readonly preco: PrecoExibido;
  readonly enderecoDaLoja: string;
}
