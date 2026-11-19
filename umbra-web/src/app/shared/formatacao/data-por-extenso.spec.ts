import { dataPorExtenso } from './data-por-extenso';

describe('dataPorExtenso', () => {
  it('escreve a data no fuso de Sao Paulo', () => {
    expect(dataPorExtenso('2026-11-19T12:00:00Z')).toBe('19 de novembro de 2026');
  });

  it('usa o dia de Sao Paulo quando em UTC ja virou o dia seguinte', () => {
    expect(dataPorExtenso('2026-11-20T02:30:00Z')).toBe('19 de novembro de 2026');
  });

  it('devolve vazio para instante invalido', () => {
    expect(dataPorExtenso('ontem')).toBe('');
  });
});
