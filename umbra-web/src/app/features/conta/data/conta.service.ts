import { Injectable, inject } from '@angular/core';
import { Api } from '../../../api/api';
import { buscarUsuarioAtual } from '../../../api/fn/usuario/buscar-usuario-atual';
import { UsuarioResponse } from '../../../api/models/usuario-response';
import { dataPorExtenso } from '../../../shared/formatacao/data-por-extenso';
import { DadosDaConta } from './dados-da-conta';

@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly api = inject(Api);

  async carregar(): Promise<DadosDaConta> {
    return paraDadosDaConta(await this.api.invoke(buscarUsuarioAtual));
  }
}

function paraDadosDaConta(usuario: UsuarioResponse): DadosDaConta {
  return {
    nome: usuario.nomeExibicao,
    email: usuario.email,
    regiao: usuario.cidade && usuario.uf ? `${usuario.cidade}/${usuario.uf}` : null,
    membroDesde: dataPorExtenso(usuario.criadoEm),
  };
}
