import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { FiguraDoAviso } from '../../../../../shared/ui/aviso/aviso.service';

export const PREFIXO_DA_PROXIMA_PIADA = 'umbra.piada.proxima.';

export interface Piada {
  readonly texto: string;
  readonly figura: FiguraDoAviso;
}

export interface FaixaDePiadas {
  readonly nome: string;
  readonly acimaDeCentavos: number;
  readonly piadas: readonly Piada[];
}

export const FAIXAS_DE_PIADAS: readonly FaixaDePiadas[] = [
  {
    nome: 'delirio',
    acimaDeCentavos: 10_000_000,
    piadas: [
      {
        texto: 'Calma, deixa eu ver se entendi: você quer pagar tudo isso… em promoção?',
        figura: { tipo: 'retrato', retrato: 'confuso' },
      },
      {
        texto: 'Procurei em todas as lojas uma peça desse valor. Achei um apartamento em Mongaguá.',
        figura: { tipo: 'sprite', estado: 'perdido' },
      },
      { texto: 'Combinado. Vou vigiar esse preço. Volto em 2089, de bengala.', figura: { tipo: 'sprite', estado: 'velhinho' } },
    ],
  },
  {
    nome: 'nao-e-peca',
    acimaDeCentavos: 2_000_000,
    piadas: [
      { texto: 'Você digitou um zero a mais ou está comprando a loja inteira?', figura: { tipo: 'sprite', estado: 'desmaiando' } },
      {
        texto: 'Esse preço é de placa de vídeo ou de satélite? Pergunto porque a entrega muda bastante.',
        figura: { tipo: 'sprite', estado: 'perdido' },
      },
      { texto: 'Isso é uma peça ou o carro inteiro com a peça dentro?', figura: { tipo: 'retrato', retrato: 'confuso' } },
    ],
  },
  {
    nome: 'caro-mas-existe',
    acimaDeCentavos: 500_000,
    piadas: [
      { texto: 'Uhul, monitoramento criado! Já posso chamar você de patrão?', figura: { tipo: 'sprite', estado: 'patrao' } },
      { texto: 'Com esse alvo dá pra levar a peça e ainda pagar o frete pra Lua.', figura: { tipo: 'retrato', retrato: 'surpreso' } },
      { texto: 'Anotado. Também anotei que você não pechincha.', figura: { tipo: 'sprite', estado: 'digitando' } },
    ],
  },
];

export function faixaDoAlvo(centavos: number): FaixaDePiadas | null {
  return FAIXAS_DE_PIADAS.find((faixa) => centavos > faixa.acimaDeCentavos) ?? null;
}

@Injectable({ providedIn: 'root' })
export class RodizioDePiadas {
  private readonly armazenamento = armazenamentoDe(inject(DOCUMENT));
  private readonly posicoesNaMemoria = new Map<string, number>();

  proxima(faixa: FaixaDePiadas): Piada {
    const posicao = this.ler(faixa.nome) % faixa.piadas.length;
    this.gravar(faixa.nome, (posicao + 1) % faixa.piadas.length);
    return faixa.piadas[posicao];
  }

  private ler(nome: string): number {
    const naMemoria = this.posicoesNaMemoria.get(nome) ?? 0;
    try {
      const guardado = this.armazenamento?.getItem(PREFIXO_DA_PROXIMA_PIADA + nome);
      const posicao = guardado === null || guardado === undefined ? Number.NaN : Number(guardado);
      return Number.isInteger(posicao) && posicao >= 0 ? posicao : naMemoria;
    } catch {
      return naMemoria;
    }
  }

  private gravar(nome: string, posicao: number): void {
    this.posicoesNaMemoria.set(nome, posicao);
    try {
      this.armazenamento?.setItem(PREFIXO_DA_PROXIMA_PIADA + nome, String(posicao));
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
