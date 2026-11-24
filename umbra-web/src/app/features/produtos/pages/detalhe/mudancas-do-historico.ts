import { diaEMes, diaMesEAno } from '../../../../shared/formatacao/data-curta';
import { reais } from '../../../../shared/formatacao/reais';
import { MudancaExibida } from '../../components/lista-de-mudancas/mudanca-exibida';
import { HistoricoDoProduto, PontoDoHistorico, SerieDaLoja } from '../../data/historico-do-produto';
import { DIA_MS } from './historico-exibido';

export const MUDANCAS_PARA_MOSTRAR_O_GRAFICO = 2;
export const MAXIMO_DE_MUDANCAS_NA_LISTA = 5;
export const DIAS_PARA_DADO_ANTIGO = 2;

interface Mudanca {
  readonly instante: number;
  readonly de: number | null;
  readonly para: number | null;
}

export function mudancas(pontos: readonly PontoDoHistorico[]): Mudanca[] {
  const resultado: Mudanca[] = [];
  pontos.forEach((ponto, posicao) => {
    const anterior = pontos[posicao - 1];
    if (anterior !== undefined && anterior.valorCentavos !== ponto.valorCentavos) {
      resultado.push({ instante: ponto.instante, de: anterior.valorCentavos, para: ponto.valorCentavos });
    }
  });
  return resultado;
}

export function mostraGrafico(pontos: readonly PontoDoHistorico[]): boolean {
  return mudancas(pontos).length >= MUDANCAS_PARA_MOSTRAR_O_GRAFICO;
}

export function lojasQueMudaram(series: readonly SerieDaLoja[]): string[] {
  return series.filter((serie) => mudancas(serie.pontos).length > 0).map((serie) => serie.loja);
}

export function mudancasPorLoja(series: readonly SerieDaLoja[]): MudancaExibida[] {
  return series
    .flatMap((serie) => mudancas(serie.pontos).map((mudanca) => ({ ...mudanca, loja: serie.loja })))
    .sort((a, b) => b.instante - a.instante || a.loja.localeCompare(b.loja))
    .slice(0, MAXIMO_DE_MUDANCAS_NA_LISTA)
    .map((mudanca) => ({ data: diaEMes(mudanca.instante), texto: `${mudanca.loja} ${descricao(mudanca)}` }));
}

export function fraseSemGrafico(historico: HistoricoDoProduto): string | null {
  const desde = historico.observadoDesde;
  const lista = mudancas(historico.melhorPreco);
  if (desde === null || lista.length >= MUDANCAS_PARA_MOSTRAR_O_GRAFICO) {
    return null;
  }
  const unica = lista[0];
  if (unica === undefined) {
    return `Sem mudança de preço desde ${diaMesEAno(desde)}`;
  }
  return `Mudou uma vez: ${descricao(unica)} em ${diaEMes(unica.instante)}`;
}

export function verificacao(verificadoEm: readonly number[], agora: number): { readonly texto: string; readonly antiga: boolean } | null {
  const validas = verificadoEm.filter((instante) => Number.isFinite(instante));
  if (validas.length === 0) {
    return null;
  }
  const ultima = Math.max(...validas);
  const quando = diaMesEAno(ultima) === diaMesEAno(agora) ? 'hoje' : `em ${diaMesEAno(ultima)}`;
  return { texto: `Verificado ${quando}`, antiga: agora - ultima > DIAS_PARA_DADO_ANTIGO * DIA_MS };
}

function descricao({ de, para }: Mudanca): string {
  if (para === null) {
    return 'ficou indisponível';
  }
  if (de === null) {
    return `voltou com ${valor(para)}`;
  }
  const sentido = para < de ? 'caiu' : 'subiu';
  const percentual = de > 0 ? ` (${para < de ? '-' : '+'}${Math.round((Math.abs(para - de) / de) * 100)}%)` : '';
  return `${sentido} para ${valor(para)}${percentual}`;
}

function valor(centavos: number): string {
  return centavos === 0 ? 'Grátis' : reais(centavos);
}
