import { reais, reaisSemQuebra } from './reais';

describe('reais', () => {
  it('formata centavos em reais no padrao brasileiro', () => {
    expect(reais(19990)).toBe('R$ 199,90');
    expect(reais(123456789)).toBe('R$ 1.234.567,89');
    expect(reais(5)).toBe('R$ 0,05');
  });

  it('sem quebra une o simbolo e o valor com espaco inseparavel', () => {
    expect(reaisSemQuebra(899999)).toBe('R$\u00a08.999,99');
  });
});
