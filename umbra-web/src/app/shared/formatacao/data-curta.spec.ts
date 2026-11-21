import { diaEMes, diaMesEAno, hora } from './data-curta';

describe('data curta', () => {
  const instante = Date.parse('2026-08-08T02:30:00Z');

  it('usa o fuso de São Paulo para dia e mês', () => {
    expect(diaEMes(instante)).toBe('07/08');
  });

  it('inclui o ano quando pedido', () => {
    expect(diaMesEAno(instante)).toBe('07/08/2026');
  });

  it('mostra a hora no fuso de São Paulo', () => {
    expect(hora(instante)).toBe('23:30');
  });
});
