import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';
import { validarConfiguracao } from './validar-configuracao';

describe('validarConfiguracao', () => {
  it('aceita origem https e devolve sem barra final', () => {
    const configuracao = validarConfiguracao({ apiUrl: 'https://api.umbra.davidcaetano.tech/', loginGoogle: false });

    expect(configuracao).toEqual({ apiUrl: 'https://api.umbra.davidcaetano.tech', loginGoogle: false });
  });

  it('aceita http com porta, o caso do desenvolvimento local', () => {
    expect(validarConfiguracao({ apiUrl: 'http://localhost:8080', loginGoogle: true }).apiUrl).toBe('http://localhost:8080');
  });

  it.each([
    ['nulo', null],
    ['texto', 'https://api'],
    ['sem apiUrl', { loginGoogle: true }],
    ['apiUrl relativa', { apiUrl: '/api', loginGoogle: true }],
    ['protocolo javascript', { apiUrl: 'javascript:alert(1)', loginGoogle: true }],
    ['protocolo ftp', { apiUrl: 'ftp://api.umbra.davidcaetano.tech', loginGoogle: true }],
    ['com caminho', { apiUrl: 'https://api.umbra.davidcaetano.tech/v1', loginGoogle: true }],
    ['com consulta', { apiUrl: 'https://api.umbra.davidcaetano.tech/?x=1', loginGoogle: true }],
    ['loginGoogle como texto', { apiUrl: 'https://api.umbra.davidcaetano.tech', loginGoogle: 'false' }],
  ])('recusa %s', (_caso, bruta) => {
    expect(() => validarConfiguracao(bruta)).toThrow(ConfiguracaoInvalidaError);
  });
});
