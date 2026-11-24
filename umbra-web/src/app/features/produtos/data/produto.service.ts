import { Injectable, inject } from '@angular/core';
import { Api } from '../../../api/api';
import { detalharProduto } from '../../../api/fn/catalogo/detalhar-produto';
import { historicoDoProduto } from '../../../api/fn/catalogo/historico-do-produto';
import { HistoricoResponse } from '../../../api/models/historico-response';
import { OfertaResponse } from '../../../api/models/oferta-response';
import { PontoHistoricoResponse } from '../../../api/models/ponto-historico-response';
import { ProdutoResponse } from '../../../api/models/produto-response';
import { CategoriaDeHardware } from './filtro-da-busca';
import { HistoricoDoProduto, PeriodoDoHistorico, PontoDoHistorico } from './historico-do-produto';
import { LARGURAS_DA_FAIXA_EM_DESTAQUE, capaPelaOrigem } from '../../../shared/capa/imagem-pela-origem';
import { precoDaOferta } from './preco-da-oferta';
import { OfertaDaLoja, ProdutoDetalhado } from './produto-detalhado';
import { ROTULO_DA_CATEGORIA } from './rotulos';

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private readonly api = inject(Api);

  async detalhar(id: number): Promise<ProdutoDetalhado> {
    return paraProdutoDetalhado(await this.api.invoke(detalharProduto, { id }));
  }

  async historico(id: number, periodo: PeriodoDoHistorico): Promise<HistoricoDoProduto> {
    return paraHistorico(await this.api.invoke(historicoDoProduto, { id, periodo }), periodo);
  }
}

function paraProdutoDetalhado(produto: ProdutoResponse): ProdutoDetalhado {
  return {
    id: produto.id,
    nome: produto.nome,
    tipo: produto.tipo,
    capa: capaPelaOrigem(produto.imagemUrl, LARGURAS_DA_FAIXA_EM_DESTAQUE),
    categoria: produto.categoria ? (ROTULO_DA_CATEGORIA[produto.categoria as CategoriaDeHardware] ?? null) : null,
    melhorOferta: produto.melhorPreco ? paraOferta(produto.melhorPreco) : null,
    ofertas: produto.ofertas.map(paraOferta).sort((a, b) => a.valorCentavos - b.valorCentavos),
  };
}

function paraOferta(oferta: OfertaResponse): OfertaDaLoja {
  return {
    loja: oferta.loja,
    valorCentavos: oferta.gratuito ? 0 : oferta.valorCentavos,
    preco: precoDaOferta(oferta),
    gratuito: oferta.gratuito,
    endereco: oferta.url,
    coletadoEm: oferta.coletadoEm,
    expiraEm: oferta.expiraEm === null ? null : Date.parse(oferta.expiraEm),
    verificadoEm: Date.parse(oferta.verificadoEm),
    situacao: oferta.situacao,
  };
}

function paraHistorico(resposta: HistoricoResponse, periodo: PeriodoDoHistorico): HistoricoDoProduto {
  const minimo = resposta.minimo;
  return {
    periodo,
    consultadoEm: Date.parse(resposta.consultadoEm),
    observadoDesde: resposta.observadoDesde === null ? null : Date.parse(resposta.observadoDesde),
    melhorPreco: resposta.melhorPreco.map(paraPonto),
    porLoja: resposta.porLoja.map((serie) => ({ loja: serie.loja, pontos: serie.pontos.map(paraPonto) })),
    minimo:
      minimo === null
        ? null
        : { valorCentavos: minimo.gratuito ? 0 : minimo.valorCentavos, loja: minimo.loja, em: Date.parse(minimo.em) },
  };
}

function paraPonto(ponto: PontoHistoricoResponse): PontoDoHistorico {
  return { instante: Date.parse(ponto.instante), valorCentavos: ponto.valorCentavos };
}
