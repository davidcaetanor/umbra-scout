import { diaEMes } from '../../../../shared/formatacao/data-curta';
import { OfertaDaLoja } from '../../data/produto-detalhado';
import { SituacaoExibida } from '../../components/tabela-lojas/oferta-exibida';

export function situacaoExibida(oferta: OfertaDaLoja): SituacaoExibida | null {
  switch (oferta.situacao) {
    case 'vigente':
      return null;
    case 'promocao-no-prazo':
      return { texto: `Promoção até ${prazo(oferta.expiraEm)}`, aviso: false };
    case 'verificacao-atrasada':
      return { texto: `Última verificação em ${diaEMes(oferta.verificadoEm)}, pode estar desatualizado`, aviso: true };
    case 'promocao-vencida-sem-coleta':
      return { texto: `Promoção terminou em ${prazo(oferta.expiraEm)}, aguardando nova verificação`, aviso: true };
  }
}

function prazo(expiraEm: number | null): string {
  return expiraEm === null ? '' : diaEMes(expiraEm);
}
