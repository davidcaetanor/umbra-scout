import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, timeout } from 'rxjs';
import { CONFIGURACAO_AMBIENTE } from '../ambiente';
import { converterErroHttp } from './converter-erro-http';
import { ehRotaDeAutenticacao } from './rotas-de-autenticacao';

export const PRAZO_DA_API_EM_MS = 15_000;

export const erroInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const origemDaApi = inject(CONFIGURACAO_AMBIENTE).apiUrl;
  if (!requisicao.url.startsWith(`${origemDaApi}/`)) {
    return proximo(requisicao);
  }
  const rotaDeAutenticacao = ehRotaDeAutenticacao(requisicao.url.slice(origemDaApi.length));
  return proximo(requisicao).pipe(
    timeout({ each: PRAZO_DA_API_EM_MS }),
    catchError((erro: unknown) => throwError(() => converterErroHttp(erro, rotaDeAutenticacao))),
  );
};
