import { imagemPelaOrigem } from './imagem-pela-origem';

describe('imagemPelaOrigem', () => {
  it('troca o CDN do ITAD pelo caminho da nossa origem, mantendo a consulta', () => {
    expect(imagemPelaOrigem('https://assets.isthereanydeal.com/018d/boxart.jpg?t=1')).toBe('/imagens/itad/018d/boxart.jpg?t=1');
  });

  it('descarta imagem de qualquer outra origem, que a CSP recusaria', () => {
    expect(imagemPelaOrigem('https://outro.exemplo.com/a.jpg')).toBeNull();
    expect(imagemPelaOrigem('https://assets.isthereanydeal.com.malicioso.example/a.jpg')).toBeNull();
    expect(imagemPelaOrigem(null)).toBeNull();
  });
});
