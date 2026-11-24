import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

export const CHAVE_DO_RASCUNHO = 'umbra.rascunho.alvo';

interface Rascunho {
  readonly alvo: string;
  readonly alvoCentavos: number;
}

interface RascunhoAntigo {
  readonly produtoId: number;
  readonly alvoCentavos: number;
}

export function rascunhoDoProduto(produtoId: number): string {
  return `produto:${produtoId}`;
}

export function rascunhoDoJogoExterno(gid: string): string {
  return `gid:${gid}`;
}

@Injectable({ providedIn: 'root' })
export class RascunhoDoAlvo {
  private readonly armazenamento = armazenamentoDe(inject(DOCUMENT));

  guardar(alvo: string, alvoCentavos: number): void {
    try {
      this.armazenamento?.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ alvo, alvoCentavos } satisfies Rascunho));
    } catch {
      return;
    }
  }

  retomar(alvo: string): number | null {
    try {
      const guardado = this.armazenamento?.getItem(CHAVE_DO_RASCUNHO);
      if (!guardado) {
        return null;
      }
      const rascunho = JSON.parse(guardado) as Partial<Rascunho & RascunhoAntigo>;
      if (chaveDe(rascunho) !== alvo) {
        return null;
      }
      this.armazenamento?.removeItem(CHAVE_DO_RASCUNHO);
      return typeof rascunho.alvoCentavos === 'number' && rascunho.alvoCentavos > 0 ? rascunho.alvoCentavos : null;
    } catch {
      return null;
    }
  }
}

function chaveDe(rascunho: Partial<Rascunho & RascunhoAntigo>): string | null {
  if (typeof rascunho.alvo === 'string') {
    return rascunho.alvo;
  }
  return typeof rascunho.produtoId === 'number' ? rascunhoDoProduto(rascunho.produtoId) : null;
}

function armazenamentoDe(documento: Document): Storage | null {
  try {
    return documento.defaultView?.sessionStorage ?? null;
  } catch {
    return null;
  }
}
