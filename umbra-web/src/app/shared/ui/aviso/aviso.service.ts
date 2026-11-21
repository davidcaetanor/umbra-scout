import { DestroyRef, Injectable, inject, signal } from '@angular/core';

export type RetratoDoAviso = 'feliz' | 'tranquilo';

export interface Aviso {
  readonly texto: string;
  readonly retrato: RetratoDoAviso;
}

export const DURACAO_DO_AVISO_MS = 6000;

@Injectable({ providedIn: 'root' })
export class AvisoService {
  private readonly visivel = signal<Aviso | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  readonly atual = this.visivel.asReadonly();

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizador));
  }

  mostrar(texto: string, retrato: RetratoDoAviso = 'feliz'): void {
    clearTimeout(this.temporizador);
    this.visivel.set({ texto, retrato });
    this.temporizador = setTimeout(() => this.fechar(), DURACAO_DO_AVISO_MS);
  }

  fechar(): void {
    clearTimeout(this.temporizador);
    this.visivel.set(null);
  }
}
