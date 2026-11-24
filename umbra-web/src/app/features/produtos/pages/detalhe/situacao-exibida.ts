import { diaEMes, diaMesEAno, hora } from '../../../../shared/formatacao/data-curta';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import { SituacaoExibida } from '../../components/tabela-lojas/oferta-exibida';

export function situacaoExibida(oferta: OfertaDaLoja, agora: number): SituacaoExibida {
  const verificacao = quandoVerificou(oferta.verificadoEm, agora);
  switch (oferta.situacao) {
    case 'vigente':
      return { texto: `Verificado ${verificacao}`, aviso: false };
    case 'promocao-no-prazo':
      return { texto: `Promoção até ${prazo(oferta.expiraEm)}; última verificação ${verificacao}`, aviso: false };
    case 'verificacao-atrasada':
      return { texto: `Última verificação ${verificacao}, pode estar desatualizado`, aviso: true };
    case 'promocao-vencida-sem-coleta':
      return { texto: `Promoção terminou em ${prazo(oferta.expiraEm)}, aguardando nova verificação`, aviso: true };
  }
}

function quandoVerificou(instante: number, agora: number): string {
  return diaMesEAno(instante) === diaMesEAno(agora) ? `hoje às ${hora(instante)}` : `em ${diaEMes(instante)}`;
}

function prazo(expiraEm: number | null): string {
  return expiraEm === null ? '' : diaEMes(expiraEm);
}
