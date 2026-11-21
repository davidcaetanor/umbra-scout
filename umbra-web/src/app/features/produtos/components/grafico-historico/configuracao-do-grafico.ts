import { ChartConfiguration, ChartDataset } from 'chart.js';
import { diaEMes } from '../../../../shared/formatacao/data-curta';
import { reais } from '../../../../shared/formatacao/reais';
import { CoresDoTema } from './cores-do-tema';
import { GraficoExibido, PontoExibido } from './grafico-exibido';

type Conjunto = ChartDataset<'line', PontoExibido[]>;

const ESPESSURA_DA_LINHA = 2;
const ESPESSURA_DO_MINIMO = 4;
const RAIO_DO_LOSANGO = 7;
const RAIO_DE_HOJE = 6;
const MAXIMO_DE_MARCAS_NO_EIXO = 6;

export function configuracaoDoGrafico(grafico: GraficoExibido, cores: CoresDoTema): ChartConfiguration<'line', PontoExibido[]> {
  const corDaLinha = { 'historico-1': cores.linha1, 'historico-3': cores.linha3 } as const;
  const linhas: Conjunto[] = grafico.linhas.map((linha) => ({
    label: linha.rotulo,
    data: [...linha.pontos],
    borderColor: corDaLinha[linha.cor],
    borderWidth: ESPESSURA_DA_LINHA,
    borderDash: linha.cor === 'historico-3' ? [6, 4] : [],
    stepped: true,
    spanGaps: false,
    pointRadius: 0,
    pointHitRadius: 8,
    pointHoverRadius: 4,
    pointHoverBackgroundColor: corDaLinha[linha.cor],
    pointHoverBorderWidth: 0,
    order: 3,
  }));
  return {
    type: 'line',
    data: { datasets: [...linhas, ...marcas(grafico, cores)] },
    options: {
      animation: false,
      responsive: true,
      maintainAspectRatio: false,
      parsing: false,
      normalized: true,
      interaction: { mode: 'nearest', axis: 'x', intersect: false },
      layout: { padding: { top: RAIO_DO_LOSANGO, right: RAIO_DO_LOSANGO } },
      font: { family: cores.fonte },
      scales: {
        x: {
          type: 'linear',
          min: grafico.inicio,
          max: grafico.fim,
          grid: { display: false },
          border: { color: cores.grade },
          ticks: {
            color: cores.rotulo,
            maxTicksLimit: MAXIMO_DE_MARCAS_NO_EIXO,
            callback: (valor, indice, marcas) => rotuloSemRepetir(Number(valor), marcas[indice - 1]?.value),
          },
        },
        y: {
          grace: '10%',
          afterDataLimits: (eixo) => {
            eixo.min = Math.max(0, eixo.min);
          },
          grid: { color: cores.grade },
          border: { display: false },
          ticks: { color: cores.rotulo, maxTicksLimit: 5, callback: (valor) => reais(Number(valor)) },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          filter: (item) => item.datasetIndex < linhas.length,
          callbacks: {
            labelColor: (item) => ({
              borderColor: item.dataset.borderColor as string,
              backgroundColor: item.dataset.borderColor as string,
              borderWidth: 2,
            }),
            title: (itens) => (itens[0] ? diaEMes(itens[0].parsed.x ?? 0) : ''),
            label: (item) => `${item.dataset.label ?? ''}: ${reais(item.parsed.y ?? 0)}`,
          },
        },
      },
    },
  };
}

function marcas(grafico: GraficoExibido, cores: CoresDoTema): Conjunto[] {
  const conjuntos: Conjunto[] = [];
  const horizontal = (valor: number): PontoExibido[] => [
    { x: grafico.inicio, y: valor },
    { x: grafico.fim, y: valor },
  ];
  if (grafico.minimo !== null) {
    conjuntos.push(
      { label: 'Apoio do menor registrado', data: horizontal(grafico.minimo.valor), borderColor: cores.apoio, borderWidth: 1, pointRadius: 0, order: 5 },
      {
        label: 'Menor registrado',
        data: [...grafico.minimo.trechos],
        borderColor: cores.minimo,
        borderWidth: ESPESSURA_DO_MINIMO,
        spanGaps: false,
        pointRadius: 0,
        order: 2,
      },
    );
    if (grafico.minimo.losango !== null) {
      conjuntos.push({
        label: 'Losango do menor registrado',
        data: [grafico.minimo.losango],
        showLine: false,
        clip: false,
        pointStyle: 'rectRot',
        pointRadius: RAIO_DO_LOSANGO,
        pointBackgroundColor: cores.minimo,
        pointBorderWidth: 0,
        order: 1,
      });
    }
  }
  if (grafico.alvo !== null) {
    conjuntos.push({
      label: 'Alvo de busca',
      data: horizontal(grafico.alvo),
      borderColor: cores.alvo,
      borderWidth: 1.5,
      borderDash: [2, 4],
      pointRadius: 0,
      order: 4,
    });
  }
  if (grafico.hoje !== null) {
    conjuntos.push({
      label: 'Hoje',
      data: [grafico.hoje],
      showLine: false,
      clip: false,
      pointRadius: RAIO_DE_HOJE,
      pointBackgroundColor: cores.hoje,
      pointBorderWidth: 0,
      order: 0,
    });
  }
  return conjuntos;
}

export function rotuloSemRepetir(valor: number, anterior: number | undefined): string | null {
  const rotulo = diaEMes(valor);
  return anterior !== undefined && diaEMes(anterior) === rotulo ? null : rotulo;
}
