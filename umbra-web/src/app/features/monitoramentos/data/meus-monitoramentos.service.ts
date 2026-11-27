import { Injectable, inject } from '@angular/core';
import { Api } from '../../../api/api';
import { alterarMonitoramento } from '../../../api/fn/monitoramento/alterar-monitoramento';
import { apagarMonitoramento } from '../../../api/fn/monitoramento/apagar-monitoramento';
import { listarMonitoramentos } from '../../../api/fn/monitoramento/listar-monitoramentos';
import { MonitoramentoResponse } from '../../../api/models/monitoramento-response';
import { OfertaResponse } from '../../../api/models/oferta-response';
import { casoDe } from '../../../core/erro';
import { capaPelaOrigem } from '../../../shared/capa/imagem-pela-origem';
import { MelhorPrecoListado, MonitoramentoListado } from './monitoramento-listado';

const TAMANHO_DA_PAGINA = 20;

@Injectable({ providedIn: 'root' })
export class MeusMonitoramentosService {
  private readonly api = inject(Api);

  async listarTodos(): Promise<MonitoramentoListado[]> {
    const itens: MonitoramentoResponse[] = [];
    let pagina = 0;
    let totalDePaginas = 1;
    while (pagina < totalDePaginas) {
      const resposta = await this.api.invoke(listarMonitoramentos, { page: pagina, size: TAMANHO_DA_PAGINA });
      itens.push(...resposta.conteudo);
      totalDePaginas = resposta.totalPaginas;
      pagina++;
    }
    return itens.map(paraListado);
  }

  async definirAtivo(id: number, ativo: boolean): Promise<MonitoramentoListado> {
    return paraListado(await this.api.invoke(alterarMonitoramento, { id, body: { ativo } }));
  }

  async alterarAlvo(id: number, precoAlvoCentavos: number): Promise<MonitoramentoListado> {
    return paraListado(await this.api.invoke(alterarMonitoramento, { id, body: { precoAlvoCentavos } }));
  }

  async remover(id: number): Promise<void> {
    try {
      await this.api.invoke(apagarMonitoramento, { id });
    } catch (erro) {
      if (casoDe(erro).tipo !== 'nao-encontrado') {
        throw erro;
      }
    }
  }
}

function paraListado(monitoramento: MonitoramentoResponse): MonitoramentoListado {
  const capa = capaPelaOrigem(monitoramento.imagemUrl);
  return {
    id: monitoramento.id,
    produtoId: monitoramento.produtoId,
    nome: monitoramento.nome,
    capa,
    tipo: capa?.enquadramento === 'foto' ? 'HARDWARE' : 'JOGO',
    precoAlvoCentavos: monitoramento.precoAlvoCentavos,
    ativo: monitoramento.ativo,
    melhorPreco: monitoramento.melhorPreco === null ? null : paraMelhorPreco(monitoramento.melhorPreco),
    distanciaDoAlvoCentavos: monitoramento.distanciaDoAlvoCentavos,
    alvoAtingido: monitoramento.ativo && monitoramento.alvoAtingido,
    avisadoEm: monitoramento.ultimoAvisoEm == null ? null : Date.parse(monitoramento.ultimoAvisoEm),
    foraDeVenda: monitoramento.aparentementeDescontinuado,
  };
}

function paraMelhorPreco(oferta: OfertaResponse): MelhorPrecoListado {
  return {
    valorCentavos: oferta.valorCentavos,
    loja: oferta.loja,
    gratuito: oferta.gratuito,
    situacao: oferta.situacao,
    expiraEm: oferta.expiraEm === null ? null : Date.parse(oferta.expiraEm),
    verificadoEm: Date.parse(oferta.verificadoEm),
  };
}
