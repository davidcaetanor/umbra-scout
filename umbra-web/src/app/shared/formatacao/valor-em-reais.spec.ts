import { lerReais, reaisParaCampo } from './valor-em-reais';

describe('valor em reais', () => {
  it.each([
    ['0', 0],
    ['49', 4900],
    ['49,9', 4990],
    ['49.90', 4990],
    ['1.500,00', 150000],
    ['1,500.00', 150000],
    ['R$ 12,50', 1250],
  ])('le %s como %d centavos', (texto, centavos) => {
    expect(lerReais(texto)).toEqual({ tipo: 'valido', centavos });
  });

  it.each(['abc', '-5', '1.2345', '12,5,0', '99999999999999'])('recusa %s', (texto) => {
    expect(lerReais(texto)).toEqual({ tipo: 'invalido' });
  });

  it('vazio e escrita para o campo', () => {
    expect(lerReais('  ')).toEqual({ tipo: 'vazio' });
    expect(reaisParaCampo(5000)).toBe('50');
    expect(reaisParaCampo(1250)).toBe('12,50');
  });
});
