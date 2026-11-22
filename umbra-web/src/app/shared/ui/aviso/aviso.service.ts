import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { EstadoMascote } from '../../mascote/estado-mascote';

export type RetratoDoAviso = 'feliz' | 'tranquilo' | 'confuso' | 'surpreso';

export type FiguraDoAviso =
  | { readonly tipo: 'retrato'; readonly retrato: RetratoDoAviso }
  | { readonly tipo: 'sprite'; readonly estado: EstadoMascote };

export interface Aviso {
  readonly texto: string;
  readonly detalhe: string | null;
  readonly figura: FiguraDoAviso;
}

export const DURACAO_DO_AVISO_MS = 6000;
export const DURACAO_DO_AVISO_LONGO_MS = 10000;

@Injectable({ providedIn: 'root' })
export class AvisoService {
  private readonly visivel = signal<Aviso | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  readonly atual = this.visivel.asReadonly();

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizador));
  }

  mostrar(texto: string, retrato: RetratoDoAviso = 'feliz'): void {
    this.exibir({ texto, detalhe: null, figura: { tipo: 'retrato', retrato } }, DURACAO_DO_AVISO_MS);
  }

  mostrarLongo(aviso: Aviso): void {
    this.exibir(aviso, DURACAO_DO_AVISO_LONGO_MS);
  }

  fechar(): void {
    clearTimeout(this.temporizador);
    this.visivel.set(null);
  }

  private exibir(aviso: Aviso, duracao: number): void {
    clearTimeout(this.temporizador);
    this.visivel.set(aviso);
    this.temporizador = setTimeout(() => this.fechar(), duracao);
  }
}
