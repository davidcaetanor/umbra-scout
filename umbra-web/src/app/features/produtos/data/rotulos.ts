import { CategoriaDeHardware, OrdenacaoDaBusca, TipoDeProduto } from './filtro-da-busca';

export const ROTULO_DA_CATEGORIA: Readonly<Record<CategoriaDeHardware, string>> = {
  CPU: 'Processador',
  GPU: 'Placa de vídeo',
  PLACA_MAE: 'Placa-mãe',
  MEMORIA_RAM: 'Memória RAM',
  FONTE: 'Fonte',
  SSD: 'SSD',
  HD: 'HD',
  COOLER: 'Cooler',
  KIT_HARDWARE: 'Kit de hardware',
  PLACA_INTERFACE: 'Placa de interface',
};

export const ROTULO_DO_TIPO: Readonly<Record<TipoDeProduto, string>> = {
  JOGO: 'Jogos',
  HARDWARE: 'Hardware',
};

export const ROTULO_DA_ORDENACAO: Readonly<Record<OrdenacaoDaBusca, string>> = {
  nome: 'Nome',
  menorPreco: 'Menor preço',
};
