import { nomeCurto } from './nome-curto';

describe('nomeCurto', () => {
  it.each([
    ['David Caetano Ribeiro', 'David Caetano'],
    ['David Caetano', 'David Caetano'],
    ['david', 'David'],
    ['DAVID CAETANO RIBEIRO', 'David Caetano'],
    ['  david   caetano  ', 'David Caetano'],
    ['Maria da Silva Souza', 'Maria Silva'],
    ['João dos Santos', 'João Santos'],
    ['Ana de', 'Ana'],
    ['ana-clara de souza', 'Ana-Clara Souza'],
    ['Ronald McDonald', 'Ronald McDonald'],
    ['élio ávila', 'Élio Ávila'],
    ['', ''],
    ['   ', ''],
  ])('%j vira %j', (completo, esperado) => {
    expect(nomeCurto(completo)).toBe(esperado);
  });
});
