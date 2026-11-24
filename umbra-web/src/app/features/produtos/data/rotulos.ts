import { CategoriaDeHardware, CodigoDaLoja, OrdenacaoDaBusca, TipoDeProduto } from './filtro-da-busca';

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
  OUTROS: 'Outros',
};

export const ROTULO_DO_TIPO: Readonly<Record<TipoDeProduto, string>> = {
  JOGO: 'Jogos',
  HARDWARE: 'Hardware',
};

export const ROTULO_DA_ORDENACAO: Readonly<Record<OrdenacaoDaBusca, string>> = {
  nome: 'Nome',
  menorPreco: 'Menor preço',
  melhoresOfertas: 'Melhores ofertas',
  maiorDesconto: 'Maior desconto (%)',
};

export const ROTULO_DA_LOJA: Readonly<Record<CodigoDaLoja, string>> = {
  STEAM: 'Steam',
  NUUVEM: 'Nuuvem',
  EPIC: 'Epic Games Store',
  GOG: 'GOG',
  GREEN_MAN_GAMING: 'Green Man Gaming',
  KABUM: 'KaBuM!',
  TERABYTE: 'Terabyte',
};

export const LOJAS_POR_TIPO: Readonly<Record<TipoDeProduto, readonly CodigoDaLoja[]>> = {
  JOGO: ['STEAM', 'NUUVEM', 'EPIC', 'GOG', 'GREEN_MAN_GAMING'],
  HARDWARE: ['KABUM'],
};
