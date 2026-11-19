import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { CONFIGURACAO_AMBIENTE } from '../ambiente';
import { ROTA_DE_ENTRADA } from '../navegacao/destino-interno';
import { TokenDaSessao } from '../sessao';
import { ehRotaDeAutenticacao } from './rotas-de-autenticacao';

const NAO_AUTENTICADO = 401;
export const MOTIVO_SESSAO_EXPIRADA = 'expirada';

export const autenticacaoInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const origemDaApi = inject(CONFIGURACAO_AMBIENTE).apiUrl;
  if (!requisicao.url.startsWith(`${origemDaApi}/`)) {
    return proximo(requisicao);
  }
  const tokenDaSessao = inject(TokenDaSessao);
  const router = inject(Router);
  const token = tokenDaSessao.vigente();
  const autenticada =
    token === null ? requisicao : requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  return proximo(autenticada).pipe(
    catchError((erro: unknown) => {
      const caminho = requisicao.url.slice(origemDaApi.length);
      if (erro instanceof HttpErrorResponse && erro.status === NAO_AUTENTICADO && !ehRotaDeAutenticacao(caminho)) {
        tokenDaSessao.limpar();
        void router.navigate([ROTA_DE_ENTRADA], {
          queryParams: { returnUrl: router.url, ...(token === null ? {} : { motivo: MOTIVO_SESSAO_EXPIRADA }) },
        });
      }
      return throwError(() => erro);
    }),
  );
};
