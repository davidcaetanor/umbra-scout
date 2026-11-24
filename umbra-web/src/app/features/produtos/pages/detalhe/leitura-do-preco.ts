import { diaEMes, diaMesEAno } from '../../../../shared/formatacao/data-curta';
import { LeituraExibida, TomDaLeitura } from '../../components/topo-produto/topo-exibido';
import { HistoricoDoProduto } from '../../data/historico-do-produto';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import { DIA_MS } from './historico-exibido';

export const FOLGA_DO_BOM_MOMENTO = 0.1;
export const PONTOS_PARA_AVALIAR = 3;

const TITULO: Readonly<Record<TomDaLeitura, string>> = {
  bom: 'Bom momento',
  costume: 'Preço de costume',
  'mais-barato': 'Já esteve mais barato',
  pouco: 'Pouco histórico ainda',
};

export function leituraDoPreco(oferta: OfertaDaLoja | null, historico: HistoricoDoProduto): LeituraExibida | null {
  const minimo = historico.minimo;
  if (oferta === null || minimo === null) {
    return null;
  }
  const pontos = historico.melhorPreco.filter((ponto) => ponto.valorCentavos !== null).length;
  const tom = pontos < PONTOS_PARA_AVALIAR ? 'pouco' : tomDoPreco(oferta.valorCentavos, minimo.valorCentavos, mediana(historico));
  const partes = [explicacao(tom, oferta.valorCentavos <= minimo.valorCentavos), situacaoDaOferta(oferta, historico.consultadoEm)].filter(
    (parte): parte is string => parte !== null,
  );
  return { titulo: TITULO[tom], detalhe: partes.length === 0 ? null : maiusculaInicial(partes.join(', ')), tom };
}

function tomDoPreco(hoje: number, minimo: number, medianaDoPeriodo: number | null): TomDaLeitura {
  if (hoje <= minimo * (1 + FOLGA_DO_BOM_MOMENTO)) {
    return 'bom';
  }
  return medianaDoPeriodo !== null && hoje <= medianaDoPeriodo ? 'costume' : 'mais-barato';
}

function explicacao(tom: TomDaLeitura, noMinimo: boolean): string | null {
  if (tom === 'pouco') {
    return null;
  }
  if (tom === 'bom') {
    return noMinimo ? 'no menor preço já registrado' : 'perto do menor preço já registrado';
  }
  return tom === 'costume' ? 'dentro do comum do período' : 'vale monitorar';
}

function mediana(historico: HistoricoDoProduto): number | null {
  const valores = historico.melhorPreco
    .map((ponto) => ponto.valorCentavos)
    .filter((valor): valor is number => valor !== null)
    .sort((a, b) => a - b);
  if (valores.length === 0) {
    return null;
  }
  const meio = Math.floor(valores.length / 2);
  return valores.length % 2 === 1 ? valores[meio] : (valores[meio - 1] + valores[meio]) / 2;
}

function situacaoDaOferta(oferta: OfertaDaLoja, agora: number): string | null {
  if (oferta.situacao === 'verificacao-atrasada') {
    return `preço verificado pela última vez em ${diaEMes(oferta.verificadoEm)}`;
  }
  if (oferta.situacao !== 'promocao-no-prazo' || oferta.expiraEm === null) {
    return null;
  }
  const dia = diaMesEAno(oferta.expiraEm);
  const quando =
    dia === diaMesEAno(agora) ? 'termina hoje' : dia === diaMesEAno(agora + DIA_MS) ? 'termina amanhã' : `termina em ${diaEMes(oferta.expiraEm)}`;
  return `promoção da ${oferta.loja} ${quando}`;
}

function maiusculaInicial(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
