import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ROTA_DE_ENTRADA } from '../navegacao/destino-interno';
import { TokenDaSessao } from './token-da-sessao';

export const autenticadoGuard: CanActivateFn = (_rota, estado) => {
  if (inject(TokenDaSessao).vigente() !== null) {
    return true;
  }
  return inject(Router).createUrlTree([ROTA_DE_ENTRADA], { queryParams: { returnUrl: estado.url } });
};
