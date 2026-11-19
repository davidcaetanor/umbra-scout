import { TestBed } from '@angular/core/testing';
import { RELOGIO } from './relogio';
import { CHAVE_DO_TOKEN, MARGEM_DE_EXPIRACAO_MS, TokenDaSessao } from './token-da-sessao';
import { AGORA, tokenQueExpiraEm } from './token-de-teste';

describe('TokenDaSessao', () => {
  let agora: number;

  function criar(): TokenDaSessao {
    TestBed.configureTestingModule({ providers: [{ provide: RELOGIO, useValue: () => agora }] });
    return TestBed.inject(TokenDaSessao);
  }

  beforeEach(() => {
    agora = AGORA;
    localStorage.clear();
  });

  it('retoma o token guardado que ainda vale', () => {
    const token = tokenQueExpiraEm(3_600_000);
    localStorage.setItem(CHAVE_DO_TOKEN, token);

    const sessao = criar();

    expect(sessao.token()).toBe(token);
    expect(sessao.autenticado()).toBe(true);
  });

  it('descarta na partida o token que vence dentro da margem', () => {
    localStorage.setItem(CHAVE_DO_TOKEN, tokenQueExpiraEm(MARGEM_DE_EXPIRACAO_MS - 1));

    expect(criar().token()).toBeNull();
  });

  it('descarta na partida o token ilegivel', () => {
    localStorage.setItem(CHAVE_DO_TOKEN, 'nao-e-jwt');

    expect(criar().autenticado()).toBe(false);
  });

  it('define e limpa o token no armazenamento', () => {
    const sessao = criar();
    const token = tokenQueExpiraEm(3_600_000);

    sessao.definir(token);
    expect(localStorage.getItem(CHAVE_DO_TOKEN)).toBe(token);

    sessao.limpar();
    expect(localStorage.getItem(CHAVE_DO_TOKEN)).toBeNull();
    expect(sessao.token()).toBeNull();
  });

  it('vigente limpa o token que venceu com a aplicacao aberta', () => {
    const sessao = criar();
    sessao.definir(tokenQueExpiraEm(3_600_000));

    agora += 3_600_000;

    expect(sessao.vigente()).toBeNull();
    expect(sessao.autenticado()).toBe(false);
  });

  it('acompanha a entrada e a saida feitas em outra aba', () => {
    const sessao = criar();
    const token = tokenQueExpiraEm(3_600_000);

    window.dispatchEvent(new StorageEvent('storage', { key: CHAVE_DO_TOKEN, newValue: token }));
    expect(sessao.token()).toBe(token);

    window.dispatchEvent(new StorageEvent('storage', { key: 'outra.chave', newValue: null }));
    expect(sessao.token()).toBe(token);

    window.dispatchEvent(new StorageEvent('storage', { key: null, newValue: null }));
    expect(sessao.token()).toBeNull();
  });

  it('mantem a sessao em memoria quando o armazenamento recusa gravar', () => {
    const sessao = criar();
    const gravar = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('cota', 'QuotaExceededError');
    });

    sessao.definir(tokenQueExpiraEm(3_600_000));

    expect(sessao.autenticado()).toBe(true);
    gravar.mockRestore();
  });
});
