import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { FiguraDoAviso } from '../../../../../shared/ui/aviso/aviso.service';

export const ALVO_EXORBITANTE_CENTAVOS = 500_000;
export const CHAVE_DA_PROXIMA_PIADA = 'umbra.piada.proxima';

export interface Piada {
  readonly texto: string;
  readonly figura: FiguraDoAviso;
}

export const PIADAS: readonly Piada[] = [
  {
    texto: 'Calma, deixa eu ver se entendi: você quer pagar tudo isso… em promoção?',
    figura: { tipo: 'retrato', retrato: 'confuso' },
  },
  { texto: 'Você digitou um zero a mais ou está comprando a loja inteira?', figura: { tipo: 'sprite', estado: 'desmaiando' } },
  {
    texto: 'Esse preço é de placa de vídeo ou de satélite? Pergunto porque a entrega muda bastante.',
    figura: { tipo: 'sprite', estado: 'perdido' },
  },
  { texto: 'Uhul, monitoramento criado! Já posso chamar você de patrão?', figura: { tipo: 'sprite', estado: 'patrao' } },
  {
    texto: 'Procurei em todas as lojas uma peça desse valor. Achei um apartamento em Mongaguá.',
    figura: { tipo: 'sprite', estado: 'perdido' },
  },
  { texto: 'Combinado. Vou vigiar esse preço. Volto em 2089, de bengala.', figura: { tipo: 'sprite', estado: 'velhinho' } },
];

export function alvoExorbitante(centavos: number): boolean {
  return centavos > ALVO_EXORBITANTE_CENTAVOS;
}

@Injectable({ providedIn: 'root' })
export class RodizioDePiadas {
  private readonly armazenamento = armazenamentoDe(inject(DOCUMENT));
  private posicaoNaMemoria = 0;

  proxima(): Piada {
    const posicao = this.ler() % PIADAS.length;
    this.gravar((posicao + 1) % PIADAS.length);
    return PIADAS[posicao];
  }

  private ler(): number {
    try {
      const guardado = Number(this.armazenamento?.getItem(CHAVE_DA_PROXIMA_PIADA));
      return Number.isInteger(guardado) && guardado >= 0 ? guardado : this.posicaoNaMemoria;
    } catch {
      return this.posicaoNaMemoria;
    }
  }

  private gravar(posicao: number): void {
    this.posicaoNaMemoria = posicao;
    try {
      this.armazenamento?.setItem(CHAVE_DA_PROXIMA_PIADA, String(posicao));
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
