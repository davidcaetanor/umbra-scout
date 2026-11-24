import { diaEMes } from '../../../shared/formatacao/data-curta';
import { reais } from '../../../shared/formatacao/reais';
import { MonitoramentoExibido } from './monitoramento-exibido';

const CHEIA = 100;
const FUSO = 'America/Sao_Paulo';
const DATA_LOCAL = new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: FUSO });
const MS_POR_DIA = 86_400_000;

export const FRASES_DA_RONDA: readonly string[] = [
  'Ronda feita. Nenhum preço chegou ao alvo ainda.',
  'Passei em todas as lojas. Ninguém baixou o suficiente hoje.',
  'Tudo calmo nas vitrines. Sigo vigiando.',
  'Nenhum alvo atingido por enquanto. Paciência de caçador.',
  'As lojas ainda não cederam. Eu também não.',
];

export function contagem(itens: readonly MonitoramentoExibido[]): string {
  const ativos = itens.filter((item) => item.ativo).length;
  const pausados = itens.length - ativos;
  const partes = [];
  if (ativos > 0) {
    partes.push(`${ativos} ${ativos === 1 ? 'ativo' : 'ativos'}`);
  }
  if (pausados > 0) {
    partes.push(`${pausados} ${pausados === 1 ? 'pausado' : 'pausados'}`);
  }
  return partes.join(' · ');
}

export function todosPausados(itens: readonly MonitoramentoExibido[]): boolean {
  return itens.length > 0 && itens.every((item) => !item.ativo);
}

export function chaveDaNovidade(item: MonitoramentoExibido): string {
  return `monitoramento#${item.id}#${item.melhorPreco?.valorCentavos ?? ''}`;
}

export function preenchimento(item: MonitoramentoExibido): number {
  const preco = item.melhorPreco;
  if (item.alvoAtingido) {
    return CHEIA;
  }
  if (preco === null || preco.valorCentavos <= 0) {
    return 0;
  }
  return Math.min(CHEIA, Math.round((item.precoAlvoCentavos / preco.valorCentavos) * CHEIA));
}

export function distancia(item: MonitoramentoExibido): string | null {
  const diferenca = item.distanciaDoAlvoCentavos;
  if (diferenca === null) {
    return null;
  }
  if (diferenca === 0) {
    return 'no alvo';
  }
  return diferenca < 0 ? `${reais(-diferenca)} abaixo do alvo` : `faltam ${reais(diferenca)}`;
}

export function avisoDoPreco(item: MonitoramentoExibido): string | null {
  const preco = item.melhorPreco;
  if (preco === null) {
    return null;
  }
  switch (preco.situacao) {
    case 'vigente':
    case 'verificacao-atrasada':
      return null;
    case 'promocao-no-prazo':
      return preco.expiraEm === null ? null : `Promoção até ${diaEMes(preco.expiraEm)}`;
    case 'promocao-vencida-sem-coleta':
      return preco.expiraEm === null ? 'Promoção terminou' : `Promoção terminou em ${diaEMes(preco.expiraEm)}`;
  }
}

export const MAXIMO_DO_NOME_NA_FALA = 60;
const PONTUACAO_NO_FIM = /[\s,;:\-–]+$/u;

export function nomeCurto(nome: string, maximo = MAXIMO_DO_NOME_NA_FALA): string {
  const limpo = nome.trim();
  if (limpo.length <= maximo) {
    return limpo;
  }
  const corte = limpo.slice(0, maximo);
  const ultimoEspaco = corte.lastIndexOf(' ');
  const inteiro = ultimoEspaco > 0 ? corte.slice(0, ultimoEspaco) : corte;
  return `${inteiro.replace(PONTUACAO_NO_FIM, '')}…`;
}

export function atingidosPrimeiro<T extends MonitoramentoExibido>(itens: readonly T[]): T[] {
  return [...itens.filter((item) => item.alvoAtingido), ...itens.filter((item) => !item.alvoAtingido)];
}

export function diaLocal(instante: number): string {
  return DATA_LOCAL.format(instante);
}

export function fraseDaRonda(instante: number): string {
  const dia = Math.floor(Date.parse(`${diaLocal(instante)}T00:00:00Z`) / MS_POR_DIA);
  return FRASES_DA_RONDA[dia % FRASES_DA_RONDA.length];
}
