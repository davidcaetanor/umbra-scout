import { MudancaExibida } from './mudanca-exibida';
import { resumoDasMudancas } from './resumo-das-mudancas';

const mudanca = (data: string, menor = false): MudancaExibida => ({ data, texto: 'Steam caiu', menor });

describe('resumo das mudanças', () => {
  it('mostra as três mais recentes', () => {
    const todas = [mudanca('20/11'), mudanca('17/11'), mudanca('12/11'), mudanca('07/11')];

    expect(resumoDasMudancas(todas).map((item) => item.data)).toEqual(['20/11', '17/11', '12/11']);
  });

  it('garante a do menor preço no lugar da terceira, mantendo a ordem por data', () => {
    const todas = [mudanca('20/11'), mudanca('17/11'), mudanca('12/11'), mudanca('07/11', true)];

    expect(resumoDasMudancas(todas).map((item) => item.data)).toEqual(['20/11', '17/11', '07/11']);
  });

  it('não repete a do menor quando ela já está entre as recentes', () => {
    const todas = [mudanca('20/11', true), mudanca('17/11'), mudanca('12/11'), mudanca('07/11')];

    expect(resumoDasMudancas(todas)).toHaveLength(3);
    expect(resumoDasMudancas([mudanca('20/11')])).toHaveLength(1);
  });
});
