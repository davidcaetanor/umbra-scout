import { Pipe, PipeTransform } from '@angular/core';

const FIM_DE_FRASE = /(?<=[.!?])\s+(?=\p{Lu})/u;

export function frasesEmLinhas(texto: string): string {
  return texto.trim().split(FIM_DE_FRASE).join('\n');
}

@Pipe({ name: 'frasesEmLinhas' })
export class FrasesEmLinhasPipe implements PipeTransform {
  transform(texto: string | null | undefined): string {
    return texto ? frasesEmLinhas(texto) : '';
  }
}
