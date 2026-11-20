import { ParamMap, Params } from '@angular/router';
import {
  CategoriaDeHardware,
  CodigoDaLoja,
  DESCONTOS_MINIMOS,
  FiltroDaBusca,
  ORDENACOES_SO_DE_JOGO,
  OrdenacaoDaBusca,
  TipoDeProduto,
} from '../../data/filtro-da-busca';
import { LOJAS_POR_TIPO, ROTULO_DA_CATEGORIA, ROTULO_DA_LOJA, ROTULO_DA_ORDENACAO, ROTULO_DO_TIPO } from '../../data/rotulos';
import { SituacaoDoTermo } from './situacao-do-termo';

const TAMANHO_MINIMO_DO_TERMO = 3;
const TAMANHO_MAXIMO_DO_TERMO = 100;
const CARACTERES_ACEITOS = /^[\p{L}\p{N} '\-:.&+!]+$/u;

export const FILTRO_INICIAL: FiltroDaBusca = {
  termo: null,
  tipo: null,
  categoria: null,
  ordenacao: 'nome',
  precoMin: null,
  precoMax: null,
  descontoMin: null,
  loja: null,
  pagina: 0,
};

const PRECO_MAXIMO_ACEITO_EM_REAIS = 100_000;

const APOSTROFOS_TIPOGRAFICOS = /[‘’ʼ]/gu;

export function normalizarTermo(texto: string): string {
  return texto.replace(APOSTROFOS_TIPOGRAFICOS, "'").trim();
}

export function situacaoDoTermo(texto: string): SituacaoDoTermo {
  const termo = normalizarTermo(texto);
  if (termo.length === 0) {
    return 'vazio';
  }
  if (termo.length < TAMANHO_MINIMO_DO_TERMO) {
    return 'curto';
  }
  return termo.length <= TAMANHO_MAXIMO_DO_TERMO && CARACTERES_ACEITOS.test(termo) ? 'valido' : 'invalido';
}

export function filtroDaUrl(parametros: ParamMap): FiltroDaBusca {
  const termo = normalizarTermo(parametros.get('nome') ?? '');
  const tipo = umDe(parametros.get('tipo'), ROTULO_DO_TIPO);
  const categoria = umDe(parametros.get('categoria'), ROTULO_DA_CATEGORIA);
  const pagina = Number(parametros.get('pagina'));
  const desconto = reaisInteiros(parametros.get('descontoMin'));
  return normalizarFiltro({
    termo: situacaoDoTermo(termo) === 'valido' ? termo : null,
    tipo,
    categoria,
    ordenacao: umDe(parametros.get('ordenacao'), ROTULO_DA_ORDENACAO) ?? FILTRO_INICIAL.ordenacao,
    precoMin: reaisInteiros(parametros.get('precoMin')),
    precoMax: reaisInteiros(parametros.get('precoMax')),
    descontoMin: desconto !== null && DESCONTOS_MINIMOS.includes(desconto) ? desconto : null,
    loja: umDe(parametros.get('loja'), ROTULO_DA_LOJA),
    pagina: Number.isInteger(pagina) && pagina > 1 ? pagina - 1 : 0,
  });
}

export function normalizarFiltro(filtro: FiltroDaBusca): FiltroDaBusca {
  const soJogo = filtro.tipo === 'JOGO';
  const precoMaxCoerente =
    filtro.precoMin !== null && filtro.precoMax !== null && filtro.precoMax < filtro.precoMin ? null : filtro.precoMax;
  return {
    ...filtro,
    categoria: filtro.tipo === 'JOGO' ? null : filtro.categoria,
    ordenacao: !soJogo && ORDENACOES_SO_DE_JOGO.includes(filtro.ordenacao) ? FILTRO_INICIAL.ordenacao : filtro.ordenacao,
    precoMax: precoMaxCoerente,
    descontoMin: soJogo ? filtro.descontoMin : null,
    loja: filtro.loja !== null && lojaAceitaPara(filtro.tipo, filtro.loja) ? filtro.loja : null,
  };
}

export function lojasPara(tipo: TipoDeProduto | null): readonly CodigoDaLoja[] {
  return tipo === null ? [...LOJAS_POR_TIPO.JOGO, ...LOJAS_POR_TIPO.HARDWARE] : LOJAS_POR_TIPO[tipo];
}

export function reaisInteiros(valor: string | null): number | null {
  if (valor === null || !/^\d{1,6}$/.test(valor.trim())) {
    return null;
  }
  const numero = Number(valor.trim());
  return numero <= PRECO_MAXIMO_ACEITO_EM_REAIS ? numero : null;
}

function lojaAceitaPara(tipo: TipoDeProduto | null, loja: CodigoDaLoja): boolean {
  return lojasPara(tipo).includes(loja);
}

export function parametrosDoFiltro(filtro: FiltroDaBusca): Params {
  return {
    nome: filtro.termo,
    tipo: filtro.tipo,
    categoria: filtro.categoria,
    ordenacao: filtro.ordenacao === FILTRO_INICIAL.ordenacao ? null : filtro.ordenacao,
    precoMin: filtro.precoMin,
    precoMax: filtro.precoMax,
    descontoMin: filtro.descontoMin,
    loja: filtro.loja,
    pagina: filtro.pagina > 0 ? filtro.pagina + 1 : null,
  };
}

export function mesmoFiltro(a: FiltroDaBusca, b: FiltroDaBusca): boolean {
  return (
    a.termo === b.termo &&
    a.tipo === b.tipo &&
    a.categoria === b.categoria &&
    a.ordenacao === b.ordenacao &&
    a.precoMin === b.precoMin &&
    a.precoMax === b.precoMax &&
    a.descontoMin === b.descontoMin &&
    a.loja === b.loja &&
    a.pagina === b.pagina
  );
}

function umDe<T extends TipoDeProduto | CategoriaDeHardware | OrdenacaoDaBusca | CodigoDaLoja>(
  valor: string | null,
  aceitos: Readonly<Record<T, string>>,
): T | null {
  return valor !== null && Object.hasOwn(aceitos, valor) ? (valor as T) : null;
}
