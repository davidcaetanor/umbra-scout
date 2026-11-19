import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { TimeoutError } from 'rxjs';
import { CasoDeErro } from '../erro';
import { converterErroHttp } from './converter-erro-http';

function resposta(status: number, corpo: unknown = null, headers = new HttpHeaders()): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: corpo, headers });
}

function caso(erro: unknown, rotaDeAutenticacao = false): CasoDeErro {
  return converterErroHttp(erro, rotaDeAutenticacao).caso;
}

describe('converterErroHttp', () => {
  it('status 0 vira falha de rede', () => {
    expect(caso(resposta(0))).toEqual({ tipo: 'rede' });
  });

  it('prazo estourado vira tempo esgotado', () => {
    expect(caso(new TimeoutError())).toEqual({ tipo: 'tempo-esgotado' });
  });

  it('400 leva os erros de campo do ProblemDetail', () => {
    const corpo = { status: 400, erros: [{ campo: 'email', mensagem: 'inválido' }] };

    expect(caso(resposta(400, corpo))).toEqual({ tipo: 'invalido', campos: [{ campo: 'email', mensagem: 'inválido' }] });
  });

  it('400 sem ProblemDetail vira invalido sem campos', () => {
    expect(caso(resposta(400, 'texto'))).toEqual({ tipo: 'invalido', campos: [] });
  });

  it('401 distingue rota de autenticacao de sessao encerrada', () => {
    expect(caso(resposta(401), true)).toEqual({ tipo: 'credencial-recusada' });
    expect(caso(resposta(401), false)).toEqual({ tipo: 'sessao-encerrada' });
  });

  it.each([
    [403, 'acesso-negado'],
    [404, 'nao-encontrado'],
    [500, 'inesperado'],
    [502, 'inesperado'],
  ])('%i vira %s', (status, tipo) => {
    expect(caso(resposta(status)).tipo).toBe(tipo);
  });

  it('409 e 422 levam o detail da API', () => {
    expect(caso(resposta(409, { detail: 'Email em uso.' }))).toEqual({ tipo: 'conflito', detalhe: 'Email em uso.' });
    expect(caso(resposta(422, { detail: '  ' }))).toEqual({ tipo: 'regra', detalhe: null });
  });

  it('503 le o Retry-After em segundos', () => {
    const comPrazo = resposta(503, null, new HttpHeaders({ 'Retry-After': '30' }));
    const comData = resposta(503, null, new HttpHeaders({ 'Retry-After': 'Wed, 21 Oct 2026 07:28:00 GMT' }));

    expect(caso(comPrazo)).toEqual({ tipo: 'indisponivel', tentarDeNovoEmSegundos: 30 });
    expect(caso(comData)).toEqual({ tipo: 'indisponivel', tentarDeNovoEmSegundos: null });
    expect(caso(resposta(503))).toEqual({ tipo: 'indisponivel', tentarDeNovoEmSegundos: null });
  });

  it('erro que nao veio do http vira inesperado', () => {
    expect(caso(new Error('x'))).toEqual({ tipo: 'inesperado' });
  });
});
