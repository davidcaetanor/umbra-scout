import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, effect, inject, signal, untracked } from '@angular/core';
import { Api } from '../../api/api';
import { buscarUsuarioAtual } from '../../api/fn/usuario/buscar-usuario-atual';
import { TokenDaSessao } from './token-da-sessao';
import { UsuarioDaSessao } from './usuario-da-sessao';

const CONTA_INDISPONIVEL = 404;

@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly api = inject(Api);
  private readonly tokenDaSessao = inject(TokenDaSessao);
  private readonly atual = signal<UsuarioDaSessao | null>(null);

  readonly usuario = this.atual.asReadonly();
  readonly autenticado = this.tokenDaSessao.autenticado;

  constructor() {
    effect(() => {
      const token = this.tokenDaSessao.token();
      untracked(() => (token === null ? this.atual.set(null) : void this.carregarUsuario(token)));
    });
  }

  entrar(token: string): void {
    this.tokenDaSessao.definir(token);
  }

  sair(): void {
    this.tokenDaSessao.limpar();
  }

  private async carregarUsuario(token: string): Promise<void> {
    try {
      const resposta = await this.api.invoke(buscarUsuarioAtual);
      if (this.tokenDaSessao.token() === token) {
        this.atual.set({ nome: resposta.nomeExibicao, email: resposta.email });
      }
    } catch (erro) {
      if (erro instanceof HttpErrorResponse && erro.status === CONTA_INDISPONIVEL) {
        this.sair();
      }
    }
  }
}
