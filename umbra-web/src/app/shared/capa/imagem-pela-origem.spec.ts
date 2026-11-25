import { capaPelaOrigem } from './imagem-pela-origem';

describe('capaPelaOrigem', () => {
  it('troca a capa em pe pelas faixas deitadas de mesma proporcao, pela nossa origem', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/boxart.jpg?t=1')).toEqual({
      endereco: '/imagens/itad/018d/banner300.jpg?t=1',
      alternativas: '/imagens/itad/018d/banner300.jpg?t=1 300w, /imagens/itad/018d/banner400.jpg?t=1 400w',
      enquadramento: 'faixa',
    });
  });

  it('capa em pe em PNG tambem usa as faixas, que o ITAD publica sempre em JPEG', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/boxart.png?t=2')?.endereco).toBe('/imagens/itad/018d/banner300.jpg?t=2');
  });

  it('imagem do ITAD fora da convencao segue como veio, sem variantes', () => {
    expect(capaPelaOrigem('https://assets.isthereanydeal.com/018d/outra.png')).toEqual({
      endereco: '/imagens/itad/018d/outra.png',
      alternativas: '',
      enquadramento: 'faixa',
    });
  });

  it('foto da Kabum vai pela nossa origem e pede moldura, sem variantes', () => {
    expect(capaPelaOrigem('https://images.kabum.com.br/produtos/fotos/85948/85948_index_g.jpg')).toEqual({
      endereco: '/imagens/kabum/produtos/fotos/85948/85948_index_g.jpg',
      alternativas: '',
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
