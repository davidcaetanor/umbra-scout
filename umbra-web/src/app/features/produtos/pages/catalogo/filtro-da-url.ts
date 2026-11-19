import { ParamMap, Params } from '@angular/router';
import { CategoriaDeHardware, FiltroDaBusca, OrdenacaoDaBusca, TipoDeProduto } from '../../data/filtro-da-busca';
import { ROTULO_DA_CATEGORIA, ROTULO_DA_ORDENACAO, ROTULO_DO_TIPO } from '../../data/rotulos';
import { SituacaoDoTermo } from './situacao-do-termo';

const TAMANHO_MINIMO_DO_TERMO = 3;
const TAMANHO_MAXIMO_DO_TERMO = 100;
const CARACTERES_ACEITOS = /^[\p{L}\p{N} '\-:.&+!]+$/u;

export const FILTRO_INICIAL: FiltroDaBusca = { termo: null, tipo: null, categoria: null, ordenacao: 'nome', pagina: 0 };

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
  return {
    termo: situacaoDoTermo(termo) === 'valido' ? termo : null,
    tipo,
    categoria: tipo === 'JOGO' ? null : categoria,
    ordenacao: umDe(parametros.get('ordenacao'), ROTULO_DA_ORDENACAO) ?? FILTRO_INICIAL.ordenacao,
    pagina: Number.isInteger(pagina) && pagina > 1 ? pagina - 1 : 0,
  };
}

export function parametrosDoFiltro(filtro: FiltroDaBusca): Params {
  return {
    nome: filtro.termo,
    tipo: filtro.tipo,
    categoria: filtro.categoria,
    ordenacao: filtro.ordenacao === FILTRO_INICIAL.ordenacao ? null : filtro.ordenacao,
    pagina: filtro.pagina > 0 ? filtro.pagina + 1 : null,
  };
}

export function mesmoFiltro(a: FiltroDaBusca, b: FiltroDaBusca): boolean {
  return (
    a.termo === b.termo &&
    a.tipo === b.tipo &&
    a.categoria === b.categoria &&
    a.ordenacao === b.ordenacao &&
    a.pagina === b.pagina
  );
}

function umDe<T extends TipoDeProduto | CategoriaDeHardware | OrdenacaoDaBusca>(
  valor: string | null,
  aceitos: Readonly<Record<T, string>>,
): T | null {
  return valor !== null && Object.hasOwn(aceitos, valor) ? (valor as T) : null;
}
