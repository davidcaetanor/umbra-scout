import { convertToParamMap } from '@angular/router';
import { FILTRO_INICIAL, filtroDaUrl, mesmoFiltro, normalizarTermo, parametrosDoFiltro, situacaoDoTermo } from './filtro-da-url';

describe('filtroDaUrl', () => {
  it('le os filtros validos, com a pagina da URL a partir de 1', () => {
    const parametros = convertToParamMap({
      nome: ' hollow ',
      tipo: 'HARDWARE',
      categoria: 'GPU',
      ordenacao: 'menorPreco',
      pagina: '3',
    });

    expect(filtroDaUrl(parametros)).toEqual({
      termo: 'hollow',
      tipo: 'HARDWARE',
      categoria: 'GPU',
      ordenacao: 'menorPreco',
      pagina: 2,
    });
  });

  it('ignora o que a API recusaria, sem gerar 400', () => {
    const parametros = convertToParamMap({ nome: 'ab', tipo: 'FILME', categoria: 'XYZ', ordenacao: 'preco', pagina: '-4' });

    expect(filtroDaUrl(parametros)).toEqual(FILTRO_INICIAL);
  });

  it('descarta categoria quando o tipo e jogo, regra da API', () => {
    expect(filtroDaUrl(convertToParamMap({ tipo: 'JOGO', categoria: 'GPU' })).categoria).toBeNull();
  });

  it('nao aceita nome de propriedade herdada como filtro', () => {
    expect(filtroDaUrl(convertToParamMap({ tipo: 'toString', ordenacao: 'constructor' }))).toEqual(FILTRO_INICIAL);
  });

  it('volta para a URL sem os valores padrao', () => {
    expect(parametrosDoFiltro({ ...FILTRO_INICIAL, termo: 'hollow', pagina: 1 })).toEqual({
      nome: 'hollow',
      tipo: null,
      categoria: null,
      ordenacao: null,
      pagina: 2,
    });
  });

  it('compara filtros pelo valor', () => {
    expect(mesmoFiltro({ ...FILTRO_INICIAL }, FILTRO_INICIAL)).toBe(true);
    expect(mesmoFiltro({ ...FILTRO_INICIAL, pagina: 1 }, FILTRO_INICIAL)).toBe(false);
  });
});

describe('normalizarTermo', () => {
  it('troca o apostrofo do teclado do celular pelo reto, o unico que a API aceita', () => {
    expect(normalizarTermo(' Assassin’s ')).toBe("Assassin's");
  });
});

describe('situacaoDoTermo', () => {
  it.each([
    ['', 'vazio'],
    ['  ', 'vazio'],
    ['ab', 'curto'],
    ['hollow*', 'invalido'],
    ['Assassin’s Creed', 'valido'],
    ["Assassin's Creed: Odyssey", 'valido'],
    ['a'.repeat(101), 'invalido'],
  ])('"%s" e %s', (texto, situacao) => {
    expect(situacaoDoTermo(texto)).toBe(situacao);
  });
});
