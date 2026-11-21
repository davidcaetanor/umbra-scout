import { ALVO_MAXIMO_CENTAVOS, lerAlvo, textoDoAlvo } from './preco-alvo';

describe('preço alvo', () => {
  it.each([
    ['149,90', 14990],
    ['149.90', 14990],
    ['149,9', 14990],
    ['149.9', 14990],
    ['149', 14900],
    [' R$ 72,40 ', 7240],
    ['0,01', 1],
    ['1.500,00', 150000],
    ['1,500.00', 150000],
    ['1.500', 150000],
    ['150.000', 15_000_000],
    ['150.000,00', 15_000_000],
    ['150000', 15_000_000],
  ])('lê %s como %d centavos', (texto, centavos) => {
    expect(lerAlvo(texto)).toEqual({ tipo: 'valido', centavos });
  });

  it.each(['abc', '12a', '-5', '- 5', '1.50.0', '12,345,6', '1,5.00', '1.2345', ',50', '10,'])('recusa %s como inválido', (texto) => {
    expect(lerAlvo(texto)).toEqual({ tipo: 'invalido' });
  });

  it('distingue vazio, zero e acima do máximo', () => {
    expect(lerAlvo('  ')).toEqual({ tipo: 'vazio' });
    expect(lerAlvo('0,00')).toEqual({ tipo: 'zero' });
    expect(lerAlvo('150.000,01')).toEqual({ tipo: 'acima-do-maximo' });
    expect(lerAlvo('999999999')).toEqual({ tipo: 'acima-do-maximo' });
    expect(ALVO_MAXIMO_CENTAVOS).toBe(15_000_000);
  });

  it('escreve centavos com vírgula para o campo', () => {
    expect(textoDoAlvo(14990)).toBe('149,90');
    expect(textoDoAlvo(5)).toBe('0,05');
  });
});
