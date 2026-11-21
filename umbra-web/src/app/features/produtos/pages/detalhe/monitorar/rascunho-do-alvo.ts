import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

export const CHAVE_DO_RASCUNHO = 'umbra.rascunho.alvo';

interface Rascunho {
  readonly produtoId: number;
  readonly alvoCentavos: number;
}

@Injectable({ providedIn: 'root' })
export class RascunhoDoAlvo {
  private readonly armazenamento = armazenamentoDe(inject(DOCUMENT));

  guardar(produtoId: number, alvoCentavos: number): void {
    try {
      this.armazenamento?.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ produtoId, alvoCentavos } satisfies Rascunho));
    } catch {
      return;
    }
  }

  retomar(produtoId: number): number | null {
    try {
      const guardado = this.armazenamento?.getItem(CHAVE_DO_RASCUNHO);
      if (!guardado) {
        return null;
      }
      const rascunho = JSON.parse(guardado) as Partial<Rascunho>;
      if (rascunho.produtoId !== produtoId) {
        return null;
      }
      this.armazenamento?.removeItem(CHAVE_DO_RASCUNHO);
      return typeof rascunho.alvoCentavos === 'number' && rascunho.alvoCentavos > 0 ? rascunho.alvoCentavos : null;
    } catch {
      return null;
    }
  }
}

function armazenamentoDe(documento: Document): Storage | null {
  try {
    return documento.defaultView?.sessionStorage ?? null;
  } catch {
    return null;
  }
}
