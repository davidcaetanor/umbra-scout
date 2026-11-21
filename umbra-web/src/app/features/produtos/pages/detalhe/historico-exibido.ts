import { diaEMes, diaMesEAno } from '../../../../shared/formatacao/data-curta';
import { reais } from '../../../../shared/formatacao/reais';
import { CorDaLinha, GraficoExibido, MinimoExibido, PontoExibido } from '../../components/grafico-historico/grafico-exibido';
import { CelulaDaLegenda } from '../../components/legenda-historico/legenda-exibida';
import { HistoricoDoProduto, MinimoDoHistorico, PeriodoDoHistorico, PontoDoHistorico } from '../../data/historico-do-produto';
import { OfertaDaLoja } from '../../data/produto-detalhado';

export const DIA_MS = 86_400_000;
export const MAXIMO_DE_LOJAS_NO_GRAFICO = 2;

const DIAS_DO_PERIODO: Readonly<Record<PeriodoDoHistorico, number>> = { '30d': 30, '90d': 90, '1a': 365 };
const ROTULO_DO_PERIODO: Readonly<Record<PeriodoDoHistorico, string>> = { '30d': '30 dias', '90d': '90 dias', '1a': '1 ano' };
const PERIODOS: readonly PeriodoDoHistorico[] = ['30d', '90d', '1a'];
const CORES_DAS_LOJAS: readonly CorDaLinha[] = ['historico-1', 'historico-3'];

export interface OpcaoDePeriodo {
  readonly periodo: PeriodoDoHistorico;
  readonly rotulo: string;
  readonly disponivel: boolean;
  readonly aviso: string | null;
}

export interface ContextoDoGrafico {
  readonly historico: HistoricoDoProduto;
  readonly periodo: PeriodoDoHistorico;
  readonly melhorOferta: OfertaDaLoja | null;
  readonly alvoCentavos: number | null;
  readonly lojas: readonly string[] | null;
}

export function diasObservados(historico: HistoricoDoProduto): number {
  return historico.observadoDesde === null ? 0 : Math.max(0, Math.floor((historico.consultadoEm - historico.observadoDesde) / DIA_MS));
}

export function opcoesDePeriodo(dias: number): readonly OpcaoDePeriodo[] {
  return PERIODOS.map((periodo, posicao) => {
    const anterior = PERIODOS[posicao - 1];
    const minimo = anterior === undefined ? 0 : DIAS_DO_PERIODO[anterior] + 1;
    const disponivel = dias >= minimo;
    return {
      periodo,
      rotulo: ROTULO_DO_PERIODO[periodo],
      disponivel,
      aviso: disponivel ? null : `disponível a partir de ${minimo} dias observados`,
    };
  });
}

export function periodoEfetivo(escolhido: PeriodoDoHistorico, opcoes: readonly OpcaoDePeriodo[]): PeriodoDoHistorico {
  if (opcoes.find((opcao) => opcao.periodo === escolhido)?.disponivel) {
    return escolhido;
  }
  return opcoes.filter((opcao) => opcao.disponivel).at(-1)?.periodo ?? '30d';
}

export function temHistorico(historico: HistoricoDoProduto): boolean {
  return historico.melhorPreco.some((ponto) => ponto.valorCentavos !== null);
}

export function degraus(pontos: readonly PontoDoHistorico[], fim: number): PontoExibido[] {
  const resultado: PontoExibido[] = [];
  let anterior: number | null = null;
  for (const ponto of pontos) {
    if (ponto.valorCentavos === null && anterior !== null) {
      resultado.push({ x: ponto.instante, y: anterior });
    }
    resultado.push({ x: ponto.instante, y: ponto.valorCentavos });
    anterior = ponto.valorCentavos;
  }
  if (anterior !== null) {
    resultado.push({ x: fim, y: anterior });
  }
  return resultado;
}

interface Trecho {
  readonly de: number;
  readonly ate: number;
}

export function minimoExibido(
  pontos: readonly PontoDoHistorico[],
  minimo: MinimoDoHistorico,
  inicio: number,
  fim: number,
): MinimoExibido {
  const trechos: Trecho[] = [];
  let doMinimo: Trecho | null = null;
  pontos.forEach((ponto, posicao) => {
    const de = Math.max(ponto.instante, inicio);
    const ate = pontos[posicao + 1]?.instante ?? fim;
    if (ponto.valorCentavos !== minimo.valorCentavos || ate <= de) {
      return;
    }
    trechos.push({ de, ate });
    if (minimo.em >= ponto.instante && minimo.em < ate) {
      doMinimo = { de, ate };
    }
  });
  const marcado: Trecho | null = doMinimo ?? trechos.at(-1) ?? null;
  return {
    valor: minimo.valorCentavos,
    trechos: trechos.flatMap(({ de, ate }) => [
      { x: de, y: minimo.valorCentavos },
      { x: ate, y: minimo.valorCentavos },
      { x: ate, y: null },
    ]),
    losango: marcado === null ? null : { x: (marcado.de + marcado.ate) / 2, y: minimo.valorCentavos },
  };
}

export function graficoExibido(contexto: ContextoDoGrafico): GraficoExibido {
  const { historico, periodo, melhorOferta, alvoCentavos, lojas } = contexto;
  const fim = historico.consultadoEm;
  const inicio = Math.max(fim - DIAS_DO_PERIODO[periodo] * DIA_MS, historico.observadoDesde ?? fim - DIA_MS);
  const porLoja = lojas !== null;
  const linhas = porLoja
    ? historico.porLoja
        .filter((serie) => lojas.includes(serie.loja))
        .map((serie, posicao) => ({
          rotulo: serie.loja,
          cor: CORES_DAS_LOJAS[posicao] ?? 'historico-1',
          pontos: degraus(serie.pontos, fim),
        }))
    : [{ rotulo: 'Melhor preço do dia', cor: 'historico-1' as const, pontos: degraus(historico.melhorPreco, fim) }];
  return {
    inicio,
    fim,
    linhas,
    minimo: !porLoja && historico.minimo !== null ? minimoExibido(historico.melhorPreco, historico.minimo, inicio, fim) : null,
    hoje: melhorOferta === null ? null : { x: fim, y: melhorOferta.valorCentavos },
    alvo: alvoCentavos,
    resumo: resumo(contexto),
  };
}

export function legendaExibida(contexto: ContextoDoGrafico): CelulaDaLegenda[] {
  const { historico, melhorOferta, alvoCentavos, lojas } = contexto;
  const celulas: CelulaDaLegenda[] = [
    {
      marca: 'hoje',
      titulo: `Hoje, ${diaEMes(historico.consultadoEm)}`,
      valor: melhorOferta === null ? null : melhorOferta.preco.atual,
      detalhe: melhorOferta === null ? 'Indisponível nas lojas' : melhorOferta.loja,
    },
  ];
  if (lojas === null) {
    celulas.push({ marca: 'linha-1', titulo: 'Melhor preço do dia', valor: null, detalhe: 'entre as lojas' });
  } else {
    historico.porLoja
      .filter((serie) => lojas.includes(serie.loja))
      .forEach((serie, posicao) =>
        celulas.push({ marca: posicao === 0 ? 'linha-1' : 'linha-3', titulo: serie.loja, valor: null, detalhe: 'preço do dia' }),
      );
  }
  if (lojas === null && historico.minimo !== null) {
    celulas.push({
      marca: 'minimo',
      titulo: 'Menor registrado',
      valor: valorDoMinimo(historico.minimo),
      detalhe: `${historico.minimo.loja}, ${diaMesEAno(historico.minimo.em)}`,
    });
  }
  if (alvoCentavos !== null) {
    celulas.push({ marca: 'alvo', titulo: 'Alvo de busca', valor: reais(alvoCentavos), detalhe: null });
  }
  return celulas;
}

export function lojasIniciais(historico: HistoricoDoProduto): string[] {
  return historico.porLoja.slice(0, MAXIMO_DE_LOJAS_NO_GRAFICO).map((serie) => serie.loja);
}

function valorDoMinimo(minimo: MinimoDoHistorico): string {
  return minimo.valorCentavos === 0 ? 'Grátis' : reais(minimo.valorCentavos);
}

function resumo({ historico, periodo, melhorOferta, alvoCentavos }: ContextoDoGrafico): string {
  const partes = [`Histórico de preço em ${ROTULO_DO_PERIODO[periodo]}`];
  if (historico.minimo !== null) {
    partes.push(`menor registrado ${valorDoMinimo(historico.minimo)} em ${diaMesEAno(historico.minimo.em)}`);
  }
  partes.push(melhorOferta === null ? 'hoje indisponível nas lojas' : `hoje ${melhorOferta.preco.atual}`);
  if (alvoCentavos !== null) {
    partes.push(`alvo de busca ${reais(alvoCentavos)}`);
  }
  return `${partes.join(', ')}.`;
}
