import { ConfiguracaoInvalidaError } from './configuracao-invalida.error';
import { lerConfiguracaoEmbutida } from './ler-configuracao-embutida';

function documentoCom(html: string): Document {
  return new DOMParser().parseFromString(`<html><head>${html}</head><body></body></html>`, 'text/html');
}

function bloco(conteudo: string): string {
  return `<script id="configuracao" type="application/json">${conteudo}</script>`;
}

describe('lerConfiguracaoEmbutida', () => {
  it('devolve a configuracao validada quando o bloco existe', () => {
    const documento = documentoCom(bloco('{"apiUrl":"https://api.exemplo.com","loginGoogle":true}'));

    expect(lerConfiguracaoEmbutida(documento)).toEqual({ apiUrl: 'https://api.exemplo.com', loginGoogle: true });
  });

  it('devolve null quando o bloco nao existe', () => {
    expect(lerConfiguracaoEmbutida(documentoCom('<!-- configuracao -->'))).toBeNull();
  });

  it('falha quando o bloco nao e JSON', () => {
    expect(() => lerConfiguracaoEmbutida(documentoCom(bloco('{apiUrl:')))).toThrow(ConfiguracaoInvalidaError);
  });

  it('falha quando o bloco e JSON com valor invalido', () => {
    const documento = documentoCom(bloco('{"apiUrl":"ftp://api.exemplo.com","loginGoogle":true}'));

    expect(() => lerConfiguracaoEmbutida(documento)).toThrow(ConfiguracaoInvalidaError);
  });
});
