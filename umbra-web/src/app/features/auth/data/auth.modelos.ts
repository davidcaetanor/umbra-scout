export type ProvedorExterno = 'discord' | 'google';

export interface Credenciais {
  readonly email: string;
  readonly senha: string;
}

export interface Cadastro extends Credenciais {
  readonly nome: string;
}

export interface ErroDeCampo {
  readonly campo: keyof Cadastro;
  readonly mensagem: string;
}

export type ResultadoEntrada = 'ok' | 'credenciais' | 'falha';

export type ResultadoCadastro =
  | { readonly tipo: 'ok' }
  | { readonly tipo: 'criada-sem-entrar' }
  | { readonly tipo: 'email-em-uso' }
  | { readonly tipo: 'invalido'; readonly erros: readonly ErroDeCampo[] }
  | { readonly tipo: 'falha' };

export type ResultadoTroca = 'ok' | 'codigo-invalido' | 'falha';
