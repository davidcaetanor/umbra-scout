import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { CONFIGURACAO_AMBIENTE } from '../ambiente';
import { ROTA_DE_ENTRADA } from '../navegacao/destino-interno';
import { TokenDaSessao } from '../sessao';

const NAO_AUTENTICADO = 401;
const PREFIXO_DAS_ROTAS_DE_AUTENTICACAO = '/auth/';
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
      if (erro instanceof HttpErrorResponse && erro.status === NAO_AUTENTICADO && !caminho.startsWith(PREFIXO_DAS_ROTAS_DE_AUTENTICACAO)) {
        tokenDaSessao.limpar();
        void router.navigate([ROTA_DE_ENTRADA], {
          queryParams: { returnUrl: router.url, ...(token === null ? {} : { motivo: MOTIVO_SESSAO_EXPIRADA }) },
        });
      }
      return throwError(() => erro);
    }),
  );
};
