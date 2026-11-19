import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { TimeoutError } from 'rxjs';
import { Problema } from '../../api/models/problema';
import { CasoDeErro, ErroDaApi } from '../erro';

const SEM_RESPOSTA = 0;

export function converterErroHttp(erro: unknown, rotaDeAutenticacao: boolean): ErroDaApi {
  return new ErroDaApi(casoDoErroHttp(erro, rotaDeAutenticacao));
}

function casoDoErroHttp(erro: unknown, rotaDeAutenticacao: boolean): CasoDeErro {
  if (erro instanceof TimeoutError) {
    return { tipo: 'tempo-esgotado' };
  }
  if (!(erro instanceof HttpErrorResponse)) {
    return { tipo: 'inesperado' };
  }
  const problema = problemaDe(erro);
  switch (erro.status) {
    case SEM_RESPOSTA:
      return { tipo: 'rede' };
    case 400:
      return { tipo: 'invalido', campos: problema?.erros ?? [] };
    case 401:
      return { tipo: rotaDeAutenticacao ? 'credencial-recusada' : 'sessao-encerrada' };
    case 403:
      return { tipo: 'acesso-negado' };
    case 404:
      return { tipo: 'nao-encontrado' };
    case 409:
      return { tipo: 'conflito', detalhe: detalheDe(problema) };
    case 422:
      return { tipo: 'regra', detalhe: detalheDe(problema) };
    case 503:
      return { tipo: 'indisponivel', tentarDeNovoEmSegundos: segundosDoRetryAfter(erro.headers) };
    default:
      return { tipo: 'inesperado' };
  }
}

function problemaDe(erro: HttpErrorResponse): Partial<Problema> | null {
  const corpo: unknown = erro.error;
  return typeof corpo === 'object' && corpo !== null ? (corpo as Partial<Problema>) : null;
}

function detalheDe(problema: Partial<Problema> | null): string | null {
  const detalhe = problema?.detail?.trim();
  return detalhe ? detalhe : null;
}

function segundosDoRetryAfter(cabecalhos: HttpHeaders): number | null {
  const valor = cabecalhos.get('Retry-After');
  if (valor === null || !/^\d+$/.test(valor.trim())) {
    return null;
  }
  return Number(valor);
}
