import { DESTINO_PADRAO, destinoInterno } from './destino-interno';

describe('destinoInterno', () => {
  it.each(['/monitoramentos', '/catalogo?termo=hades', '/produtos/12#historico'])(
    'aceita o caminho interno %s',
    (caminho) => {
      expect(destinoInterno(caminho)).toBe(caminho);
    },
  );

  it.each([
    ['ausente', undefined],
    ['que nao e texto', 42],
    ['absoluto para outro site', 'https://malicioso.example/'],
    ['sem barra inicial', 'monitoramentos'],
    ['relativo ao protocolo', '//malicioso.example'],
    ['com barra invertida', '/\\malicioso.example'],
    ['de volta ao login', '/entrar?returnUrl=/x'],
    ['de volta ao cadastro', '/cadastro'],
    ['de volta ao retorno do provedor', '/login/callback?codigo=x'],
  ])('troca pelo padrao o destino %s', (_caso, valor) => {
    expect(destinoInterno(valor)).toBe(DESTINO_PADRAO);
  });
});
