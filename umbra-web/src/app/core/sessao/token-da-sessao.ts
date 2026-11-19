import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { expiracaoDoToken } from './expiracao-do-token';
import { RELOGIO } from './relogio';

export const CHAVE_DO_TOKEN = 'umbra.sessao.token';
export const MARGEM_DE_EXPIRACAO_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class TokenDaSessao {
  private readonly agora = inject(RELOGIO);
  private readonly armazenamento = armazenamentoDe(inject(DOCUMENT));
  private readonly atual = signal<string | null>(null);

  readonly token = this.atual.asReadonly();
  readonly autenticado = computed(() => this.atual() !== null);

  constructor() {
    this.atual.set(this.aceitar(this.ler()));
    const janela = inject(DOCUMENT).defaultView;
    const aoMudarEmOutraAba = (evento: StorageEvent): void => {
      if (evento.key === CHAVE_DO_TOKEN || evento.key === null) {
        this.atual.set(this.aceitar(evento.newValue));
      }
    };
    janela?.addEventListener('storage', aoMudarEmOutraAba);
    inject(DestroyRef).onDestroy(() => janela?.removeEventListener('storage', aoMudarEmOutraAba));
  }

  definir(token: string): void {
    this.gravar(token);
    this.atual.set(token);
  }

  limpar(): void {
    this.gravar(null);
    this.atual.set(null);
  }

  vigente(): string | null {
    const token = this.atual();
    if (token !== null && this.aceitar(token) === null) {
      this.limpar();
      return null;
    }
    return token;
  }

  private aceitar(token: string | null): string | null {
    if (token === null) {
      return null;
    }
    const expiraEm = expiracaoDoToken(token);
    return expiraEm !== null && expiraEm - this.agora() > MARGEM_DE_EXPIRACAO_MS ? token : null;
  }

  private ler(): string | null {
    try {
      return this.armazenamento?.getItem(CHAVE_DO_TOKEN) ?? null;
    } catch {
      return null;
    }
  }

  private gravar(token: string | null): void {
    try {
      if (token === null) {
        this.armazenamento?.removeItem(CHAVE_DO_TOKEN);
      } else {
        this.armazenamento?.setItem(CHAVE_DO_TOKEN, token);
      }
    } catch {
      return;
    }
  }
}

function armazenamentoDe(documento: Document): Storage | null {
  try {
    return documento.defaultView?.localStorage ?? null;
  } catch {
    return null;
  }
}
