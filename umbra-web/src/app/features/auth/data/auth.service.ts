import { DOCUMENT } from '@angular/common';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Api } from '../../../api/api';
import { cadastrar } from '../../../api/fn/autenticacao/cadastrar';
import { entrar } from '../../../api/fn/autenticacao/entrar';
import { iniciarLoginExterno } from '../../../api/fn/autenticacao/iniciar-login-externo';
import { trocarCodigoDeLoginExterno } from '../../../api/fn/autenticacao/trocar-codigo-de-login-externo';
import { CONFIGURACAO_AMBIENTE } from '../../../core/ambiente';
import { TipoDeErro, casoDe } from '../../../core/erro';
import { SessaoService } from '../../../core/sessao';
import {
  Cadastro,
  Credenciais,
  ErroDeCampo,
  ProvedorExterno,
  ResultadoCadastro,
  ResultadoEntrada,
  ResultadoTroca,
} from './auth.modelos';

const RECUSA_DE_CREDENCIAL: readonly TipoDeErro[] = ['credencial-recusada', 'invalido'];
const CHAVE_DO_DESTINO = 'umbra.login-externo.destino';

export const SAIR_PARA = new InjectionToken<(endereco: string) => void>('SAIR_PARA', {
  providedIn: 'root',
  factory: () => {
    const documento = inject(DOCUMENT);
    return (endereco) => documento.location.assign(endereco);
  },
});

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(Api);
  private readonly sessao = inject(SessaoService);
  private readonly configuracao = inject(CONFIGURACAO_AMBIENTE);
  private readonly sairPara = inject(SAIR_PARA);
  private readonly documento = inject(DOCUMENT);
  private readonly trocas = new Map<string, Promise<ResultadoTroca>>();

  readonly provedores: readonly ProvedorExterno[] = this.configuracao.loginGoogle ? ['discord', 'google'] : ['discord'];

  async entrar(credenciais: Credenciais): Promise<ResultadoEntrada> {
    try {
      const resposta = await this.api.invoke(entrar, { body: credenciais });
      this.sessao.entrar(resposta.token);
      return 'ok';
    } catch (erro) {
      return RECUSA_DE_CREDENCIAL.includes(casoDe(erro).tipo) ? 'credenciais' : 'falha';
    }
  }

  async cadastrar(cadastro: Cadastro): Promise<ResultadoCadastro> {
    try {
      await this.api.invoke(cadastrar, {
        body: { nomeExibicao: cadastro.nome, email: cadastro.email, senha: cadastro.senha },
      });
    } catch (erro) {
      if (casoDe(erro).tipo === 'conflito') {
        return { tipo: 'email-em-uso' };
      }
      const erros = errosDeCampo(erro);
      return erros.length > 0 ? { tipo: 'invalido', erros } : { tipo: 'falha' };
    }
    const entrada = await this.entrar({ email: cadastro.email, senha: cadastro.senha });
    return entrada === 'ok' ? { tipo: 'ok' } : { tipo: 'criada-sem-entrar' };
  }

  trocarCodigo(codigo: string): Promise<ResultadoTroca> {
    const emAndamento = this.trocas.get(codigo);
    if (emAndamento !== undefined) {
      return emAndamento;
    }
    const troca = this.trocarUmaVez(codigo);
    this.trocas.set(codigo, troca);
    return troca;
  }

  private async trocarUmaVez(codigo: string): Promise<ResultadoTroca> {
    try {
      const resposta = await this.api.invoke(trocarCodigoDeLoginExterno, { body: { codigo } });
      this.sessao.entrar(resposta.token);
      return 'ok';
    } catch (erro) {
      return RECUSA_DE_CREDENCIAL.includes(casoDe(erro).tipo) ? 'codigo-invalido' : 'falha';
    }
  }

  iniciarLoginExterno(provedor: ProvedorExterno, destino: string): void {
    this.guardarDestino(destino);
    this.sairPara(`${this.configuracao.apiUrl}${iniciarLoginExterno.PATH.replace('{provedor}', provedor)}`);
  }

  retomarDestino(): string | null {
    try {
      const armazenamento = this.documento.defaultView?.sessionStorage;
      const destino = armazenamento?.getItem(CHAVE_DO_DESTINO) ?? null;
      armazenamento?.removeItem(CHAVE_DO_DESTINO);
      return destino;
    } catch {
      return null;
    }
  }

  private guardarDestino(destino: string): void {
    try {
      this.documento.defaultView?.sessionStorage.setItem(CHAVE_DO_DESTINO, destino);
    } catch {
      return;
    }
  }
}

const CAMPOS_DO_CONTRATO: Readonly<Record<string, keyof Cadastro>> = {
  nomeExibicao: 'nome',
  email: 'email',
  senha: 'senha',
};

function errosDeCampo(erro: unknown): ErroDeCampo[] {
  const caso = casoDe(erro);
  if (caso.tipo !== 'invalido') {
    return [];
  }
  return caso.campos.flatMap(({ campo, mensagem }) => {
    const nosso = CAMPOS_DO_CONTRATO[campo];
    return nosso === undefined ? [] : [{ campo: nosso, mensagem }];
  });
}
