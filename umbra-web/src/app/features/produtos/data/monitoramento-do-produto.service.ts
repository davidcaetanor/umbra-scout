import { Injectable, inject } from '@angular/core';
import { Api } from '../../../api/api';
import { alterarMonitoramento } from '../../../api/fn/monitoramento/alterar-monitoramento';
import { criarMonitoramento } from '../../../api/fn/monitoramento/criar-monitoramento';
import { listarMonitoramentos } from '../../../api/fn/monitoramento/listar-monitoramentos';
import { MonitoramentoResponse } from '../../../api/models/monitoramento-response';
import { MonitoramentoDoProduto } from './monitoramento-do-produto';

@Injectable({ providedIn: 'root' })
export class MonitoramentoDoProdutoService {
  private readonly api = inject(Api);

  async buscar(produtoId: number): Promise<MonitoramentoDoProduto | null> {
    const pagina = await this.api.invoke(listarMonitoramentos, { produtoId, size: 1 });
    const encontrado = pagina.conteudo[0];
    return encontrado === undefined ? null : paraMonitoramento(encontrado);
  }

  async criar(produtoId: number, precoAlvoCentavos: number): Promise<MonitoramentoDoProduto> {
    return paraMonitoramento(await this.api.invoke(criarMonitoramento, { body: { produtoId, precoAlvoCentavos } }));
  }

  async alterarAlvo(id: number, precoAlvoCentavos: number): Promise<MonitoramentoDoProduto> {
    return paraMonitoramento(await this.api.invoke(alterarMonitoramento, { id, body: { precoAlvoCentavos } }));
  }
}

function paraMonitoramento(monitoramento: MonitoramentoResponse): MonitoramentoDoProduto {
  return { id: monitoramento.id, precoAlvoCentavos: monitoramento.precoAlvoCentavos };
}
