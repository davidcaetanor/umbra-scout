import { MARCA_DO_CABECALHO, PASTA_DO_DESTAQUE, PASTA_DOS_SPRITES, caminhoDoRetrato } from './caminhos-da-arte';

describe('caminhos da arte', () => {
  it('monta o retrato pela mesma raiz das pastas do mascote', () => {
    const raiz = PASTA_DOS_SPRITES.slice(0, PASTA_DOS_SPRITES.lastIndexOf('/'));

    expect(caminhoDoRetrato('tranquilo')).toBe(`${raiz}/retratos/tranquilo.png`);
    expect(PASTA_DO_DESTAQUE).toBe(`${raiz}/destaque`);
  });

  it('usa caminhos absolutos para valer em qualquer rota', () => {
    for (const caminho of [PASTA_DOS_SPRITES, PASTA_DO_DESTAQUE, MARCA_DO_CABECALHO, caminhoDoRetrato('base')]) {
      expect(caminho.startsWith('/')).toBe(true);
    }
  });
});
