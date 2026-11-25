import { capaPelaOrigem } from './imagem-pela-origem';
import { LARGURAS_DO_DESTAQUE } from './larguras-da-capa';

describe('capaPelaOrigem', () => {
  it('troca a capa em pe pela faixa deitada no tamanho de cada preset, pela nossa origem', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/boxart.jpg?t=1')).toEqual({
      endereco: '/imagens/240/itad/018d/banner300.jpg?t=1',
      alternativas: '/imagens/240/itad/018d/banner300.jpg?t=1 240w, /imagens/480/itad/018d/banner600.jpg?t=1 480w',
      enquadramento: 'faixa',
    });
  });

  it('destaque acrescenta o preset de 600 sobre a maior faixa do ITAD', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/boxart.jpg?t=1', LARGURAS_DO_DESTAQUE)?.alternativas).toBe(
      '/imagens/240/itad/018d/banner300.jpg?t=1 240w, /imagens/480/itad/018d/banner600.jpg?t=1 480w, /imagens/600/itad/018d/banner600.jpg?t=1 600w',
    );
  });

  it('capa em pe em PNG tambem usa as faixas, que o ITAD publica sempre em JPEG', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/boxart.png?t=2')?.endereco).toBe('/imagens/240/itad/018d/banner300.jpg?t=2');
  });

  it('imagem do ITAD fora da convencao usa o mesmo arquivo em cada preset', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/outra.png')).toEqual({
      endereco: '/imagens/240/itad/018d/outra.png',
      alternativas: '/imagens/240/itad/018d/outra.png 240w, /imagens/480/itad/018d/outra.png 480w',
      enquadramento: 'faixa',
    });
  });

  it('foto da Kabum vai pela nossa origem em cada preset e pede moldura', () => {
    expect(capaPelaOrigem('https://images.kabum.com.br/produtos/fotos/85948/85948_index_g.jpg')).toEqual({
      endereco: '/imagens/240/kabum/produtos/fotos/85948/85948_index_g.jpg',
      alternativas:
        '/imagens/240/kabum/produtos/fotos/85948/85948_index_g.jpg 240w, /imagens/480/kabum/produtos/fotos/85948/85948_index_g.jpg 480w',
      enquadramento: 'foto',
    });
  });

  it('descarta imagem de qualquer outra origem, que a CSP recusaria', () => {
    expect(capaPelaOrigem('https://outro.exemplo.com/boxart.jpg')).toBeNull();
    expect(capaPelaOrigem('https://assets.isthereanydeal.com.malicioso.example/boxart.jpg')).toBeNull();
    expect(capaPelaOrigem('https://images.kabum.com.br.malicioso.example/x.jpg')).toBeNull();
    expect(capaPelaOrigem(null)).toBeNull();
  });
});
