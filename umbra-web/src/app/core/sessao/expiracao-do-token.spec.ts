import { expiracaoDoToken } from './expiracao-do-token';
import { AGORA, tokenQueExpiraEm } from './token-de-teste';

describe('expiracaoDoToken', () => {
  it('le o exp em milissegundos', () => {
    expect(expiracaoDoToken(tokenQueExpiraEm(3_600_000))).toBe(AGORA + 3_600_000);
  });

  it.each([
    ['sem tres segmentos', 'abc.def'],
    ['com conteudo que nao e json', 'a.bm9wZQ.c'],
    ['com conteudo que nao e objeto', `a.${btoa('42')}.c`],
    ['sem exp', `a.${btoa('{"sub":"1"}')}.c`],
    ['com exp nao numerico', `a.${btoa('{"exp":"amanha"}')}.c`],
  ])('devolve nulo para token %s', (_caso, token) => {
    expect(expiracaoDoToken(token)).toBeNull();
  });
});
