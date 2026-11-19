import { CasoDeErro } from './caso-de-erro';
import { ErroDaApi } from './erro-da-api';
import { mensagemDoCaso, mensagemDoErro } from './mensagem-do-erro';

describe('mensagemDoCaso', () => {
  it('sessao encerrada nao tem mensagem, porque o login ja explica', () => {
    expect(mensagemDoCaso({ tipo: 'sessao-encerrada' })).toBeNull();
  });

  it.each<CasoDeErro>([
    { tipo: 'rede' },
    { tipo: 'tempo-esgotado' },
    { tipo: 'credencial-recusada' },
    { tipo: 'acesso-negado' },
    { tipo: 'nao-encontrado' },
    { tipo: 'invalido', campos: [] },
    { tipo: 'inesperado' },
  ])('$tipo tem mensagem sem codigo tecnico', (caso) => {
    const mensagem = mensagemDoCaso(caso);

    expect(mensagem).toBeTruthy();
    expect(mensagem).not.toMatch(/\d{3}|http|erro interno/i);
  });

  it('conflito e regra mostram o detail da API, com mensagem propria na falta dele', () => {
    expect(mensagemDoCaso({ tipo: 'regra', detalhe: 'Preço-alvo acima do atual.' })).toBe('Preço-alvo acima do atual.');
    expect(mensagemDoCaso({ tipo: 'conflito', detalhe: null })).toBe('Não foi possível concluir esta ação.');
  });

  it('indisponivel usa o prazo quando a API informa', () => {
    expect(mensagemDoCaso({ tipo: 'indisponivel', tentarDeNovoEmSegundos: 30 })).toContain('Tente de novo em 30 segundos.');
    expect(mensagemDoCaso({ tipo: 'indisponivel', tentarDeNovoEmSegundos: 1 })).toContain('em 1 segundo.');
    expect(mensagemDoCaso({ tipo: 'indisponivel', tentarDeNovoEmSegundos: null })).toContain('Tente de novo em instantes.');
  });
});

describe('mensagemDoErro', () => {
  it('le o caso do ErroDaApi', () => {
    expect(mensagemDoErro(new ErroDaApi({ tipo: 'nao-encontrado' }))).toBe('Não encontramos o que você procurava.');
  });

  it('qualquer outro erro vira a mensagem de inesperado', () => {
    expect(mensagemDoErro(new TypeError('x'))).toContain('Algo deu errado');
  });
});
