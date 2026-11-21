import { centavosDoAlvo, textoDoAlvo } from './preco-alvo';

describe('preço alvo', () => {
  it.each([
    ['149,90', 14990],
    ['149.9', 14990],
    ['149', 14900],
    [' R$ 72,40 ', 7240],
    ['0,01', 1],
  ])('lê %s como %d centavos', (texto, centavos) => {
    expect(centavosDoAlvo(texto)).toBe(centavos);
  });

  it.each(['', '0', '0,00', 'abc', '1.000,00', '-5', '12,345'])('recusa %s', (texto) => {
    expect(centavosDoAlvo(texto)).toBeNull();
  });

  it('escreve centavos com vírgula para o campo', () => {
    expect(textoDoAlvo(14990)).toBe('149,90');
    expect(textoDoAlvo(5)).toBe('0,05');
  });
});
