import { convertToParamMap } from '@angular/router';
import {
  FILTRO_INICIAL,
  faixaInvertidaNaUrl,
  filtroDaUrl,
  mesmoFiltro,
  mesmosParametros,
  normalizarTermo,
  ordenacaoAoBuscar,
  parametrosDoFiltro,
  situacaoDoTermo,
} from './filtro-da-url';

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
      ...FILTRO_INICIAL,
      termo: 'hollow',
      tipo: 'HARDWARE',
      categoria: 'GPU',
      ordenacao: 'menorPreco',
      pagina: 2,
    });
  });

  it('le faixa de preco, desconto e loja de jogo', () => {
    const parametros = convertToParamMap({
      tipo: 'JOGO',
      ordenacao: 'melhoresOfertas',
      precoMin: '10',
      precoMax: '200',
      descontoMin: '50',
      loja: 'STEAM',
    });

    expect(filtroDaUrl(parametros)).toEqual({
      ...FILTRO_INICIAL,
      tipo: 'JOGO',
      ordenacao: 'melhoresOfertas',
      precoMin: 1000,
      precoMax: 20000,
      descontoMin: 50,
      loja: 'STEAM',
    });
  });

  it('le centavos com ponto na URL e devolve a URL em reais', () => {
    const filtro = filtroDaUrl(convertToParamMap({ precoMin: '12.5', precoMax: '99.99' }));

    expect(filtro).toMatchObject({ precoMin: 1250, precoMax: 9999 });
    expect(parametrosDoFiltro(filtro)).toMatchObject({ precoMin: '12.5', precoMax: '99.99' });
    expect(filtroDaUrl(convertToParamMap({ precoMin: '12,5' })).precoMin).toBeNull();
    expect(filtroDaUrl(convertToParamMap({ precoMin: '100000' })).precoMin).toBe(10_000_000);
    expect(filtroDaUrl(convertToParamMap({ precoMin: '100000.01' })).precoMin).toBeNull();
  });

  it('reconhece faixa invertida na URL e compara parametros sem os vazios', () => {
    expect(faixaInvertidaNaUrl(convertToParamMap({ precoMin: '300', precoMax: '100' }))).toBe(true);
    expect(faixaInvertidaNaUrl(convertToParamMap({ precoMin: '100', precoMax: '100' }))).toBe(false);
    expect(mesmosParametros(convertToParamMap({ tipo: 'JOGO' }), { tipo: 'JOGO', loja: null })).toBe(true);
    expect(mesmosParametros(convertToParamMap({ tipo: 'JOGO', pagina: 'abc' }), { tipo: 'JOGO' })).toBe(false);
    expect(mesmosParametros(convertToParamMap({ precoMin: '050' }), { precoMin: '50' })).toBe(false);
  });

  it('a Terabyte nao e aceita enquanto nao for coletada', () => {
    expect(filtroDaUrl(convertToParamMap({ tipo: 'HARDWARE', loja: 'TERABYTE' })).loja).toBeNull();
  });

  it('criterio de oferta e desconto fora de jogo voltam ao padrao, como a API exigiria', () => {
    const parametros = convertToParamMap({ tipo: 'HARDWARE', ordenacao: 'maiorDesconto', descontoMin: '50', loja: 'STEAM' });

    expect(filtroDaUrl(parametros)).toEqual({ ...FILTRO_INICIAL, tipo: 'HARDWARE' });
  });

  it('descarta preco invalido, desconto fora da lista e faixa invertida', () => {
    const parametros = convertToParamMap({ tipo: 'JOGO', precoMin: '300', precoMax: '100', descontoMin: '33' });
    expect(filtroDaUrl(parametros)).toEqual({ ...FILTRO_INICIAL, tipo: 'JOGO', precoMin: 30000 });

    expect(filtroDaUrl(convertToParamMap({ precoMin: '-5', precoMax: '1e3' }))).toEqual(FILTRO_INICIAL);
    expect(filtroDaUrl(convertToParamMap({ precoMax: '9999999' }))).toEqual(FILTRO_INICIAL);
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
    expect(parametrosDoFiltro({ ...FILTRO_INICIAL, termo: 'hollow', ordenacao: 'nome', pagina: 1 })).toEqual({
      nome: 'hollow',
      tipo: null,
      categoria: null,
      ordenacao: null,
      precoMin: null,
      precoMax: null,
      descontoMin: null,
      loja: null,
      pagina: 2,
    });
  });

  it('abre em melhores ofertas e uma busca passa para a ordem por nome', () => {
    expect(FILTRO_INICIAL.ordenacao).toBe('melhoresOfertas');
    expect(filtroDaUrl(convertToParamMap({})).ordenacao).toBe('melhoresOfertas');
    expect(filtroDaUrl(convertToParamMap({ nome: 'hollow' })).ordenacao).toBe('nome');
    expect(filtroDaUrl(convertToParamMap({ nome: 'hollow', ordenacao: 'melhoresOfertas' })).ordenacao).toBe('melhoresOfertas');
    expect(parametrosDoFiltro(FILTRO_INICIAL)['ordenacao']).toBeNull();
    expect(parametrosDoFiltro({ ...FILTRO_INICIAL, ordenacao: 'nome' })['ordenacao']).toBe('nome');
  });

  it('ao buscar, troca so a ordem padrao e respeita a escolhida', () => {
    expect(ordenacaoAoBuscar(FILTRO_INICIAL, 'hollow')).toBe('nome');
    expect(ordenacaoAoBuscar({ ...FILTRO_INICIAL, termo: 'hollow', ordenacao: 'nome' }, null)).toBe('melhoresOfertas');
    expect(ordenacaoAoBuscar({ ...FILTRO_INICIAL, ordenacao: 'menorPreco' }, 'hollow')).toBe('menorPreco');
  });

  it('melhores ofertas vale para hardware; maior desconto continua so de jogo', () => {
    expect(filtroDaUrl(convertToParamMap({ tipo: 'HARDWARE', ordenacao: 'melhoresOfertas' })).ordenacao).toBe('melhoresOfertas');
    expect(filtroDaUrl(convertToParamMap({ tipo: 'HARDWARE', ordenacao: 'maiorDesconto' })).ordenacao).toBe('melhoresOfertas');
  });

  it('compara filtros pelo valor', () => {
    expect(mesmoFiltro({ ...FILTRO_INICIAL }, FILTRO_INICIAL)).toBe(true);
    expect(mesmoFiltro({ ...FILTRO_INICIAL, pagina: 1 }, FILTRO_INICIAL)).toBe(false);
    expect(mesmoFiltro({ ...FILTRO_INICIAL, loja: 'GOG' }, FILTRO_INICIAL)).toBe(false);
    expect(mesmoFiltro({ ...FILTRO_INICIAL, precoMax: 50 }, FILTRO_INICIAL)).toBe(false);
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
