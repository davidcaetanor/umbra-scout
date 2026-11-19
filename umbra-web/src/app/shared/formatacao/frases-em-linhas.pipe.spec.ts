import { FrasesEmLinhasPipe, frasesEmLinhas } from './frases-em-linhas.pipe';

describe('frasesEmLinhas', () => {
  it('poe cada frase na sua linha', () => {
    expect(frasesEmLinhas('Umbra observa este produto há 12 dias. O menor preço foi na Steam, em 03/11/2026.')).toBe(
      'Umbra observa este produto há 12 dias.\nO menor preço foi na Steam, em 03/11/2026.',
    );
  });

  it('quebra depois de exclamacao e interrogacao e aceita inicial acentuada', () => {
    expect(frasesEmLinhas('Pronto! Ótimo preço? Sim.')).toBe('Pronto!\nÓtimo preço?\nSim.');
  });

  it('nao quebra ponto de data, de milhar nem de abreviacao seguida de minuscula', () => {
    expect(frasesEmLinhas('Em 03.11.2026 custava R$ 1.234,00 e caiu p. ex. no Steam.')).toBe(
      'Em 03.11.2026 custava R$ 1.234,00 e caiu p. ex. no Steam.',
    );
  });

  it('o pipe devolve vazio para valor ausente', () => {
    const pipe = new FrasesEmLinhasPipe();
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(' Uma frase. Outra. ')).toBe('Uma frase.\nOutra.');
  });
});
