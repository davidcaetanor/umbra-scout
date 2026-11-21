import { OfertaResponse } from '../../../api/models/oferta-response';
import { reais } from '../../../shared/formatacao/reais';
import { PrecoDoProduto } from './preco-do-produto';

export function precoDaOferta(oferta: OfertaResponse): PrecoDoProduto {
  const emPromocao =
    !oferta.gratuito &&
    !oferta.promocaoEncerrada &&
    (oferta.descontoPct ?? 0) > 0 &&
    oferta.valorOriginalCentavos !== null &&
    oferta.valorOriginalCentavos > oferta.valorCentavos;
  return {
    atual: oferta.gratuito ? 'Grátis' : reais(oferta.valorCentavos),
    original: emPromocao && oferta.valorOriginalCentavos !== null ? reais(oferta.valorOriginalCentavos) : null,
    descontoPct: emPromocao ? oferta.descontoPct : null,
    loja: oferta.loja,
  };
}
