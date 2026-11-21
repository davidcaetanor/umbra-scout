import { diaEMes, diaMesEAno } from '../../../../shared/formatacao/data-curta';
import { reais } from '../../../../shared/formatacao/reais';
import { LeituraExibida } from '../../components/topo-produto/topo-exibido';
import { MinimoDoHistorico } from '../../data/historico-do-produto';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import { DIA_MS } from './historico-exibido';

export function leituraDoPreco(oferta: OfertaDaLoja | null, minimo: MinimoDoHistorico | null, agora: number): LeituraExibida | null {
  if (oferta === null || minimo === null) {
    return null;
  }
  const diferenca = oferta.valorCentavos - minimo.valorCentavos;
  const noMinimo = diferenca <= 0;
  const partes = [noMinimo ? null : percentualAcima(diferenca, minimo.valorCentavos), prazo(oferta, agora)].filter(
    (parte): parte is string => parte !== null,
  );
  return {
    titulo: noMinimo ? 'No menor preço já registrado' : `${reais(diferenca)} acima do menor registrado`,
    detalhe: partes.length === 0 ? null : maiusculaInicial(partes.join(', ')),
    noMinimo,
  };
}

function percentualAcima(diferenca: number, minimo: number): string | null {
  return minimo > 0 ? `+${Math.round((diferenca / minimo) * 100)}%` : null;
}

function prazo(oferta: OfertaDaLoja, agora: number): string | null {
  if (oferta.expiraEm === null || oferta.expiraEm <= agora) {
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
