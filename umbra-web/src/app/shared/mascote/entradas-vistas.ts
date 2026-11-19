import { Injectable } from '@angular/core';

const CHAVE = 'umbra.mascote.entradas-vistas';

@Injectable({ providedIn: 'root' })
export class EntradasVistas {
  private readonly vistas = new Set<string>(this.ler());

  jaViu(entrada: string): boolean {
    return this.vistas.has(entrada);
  }

  registrar(entrada: string): void {
    this.vistas.add(entrada);
    this.gravar();
  }

  private ler(): string[] {
    try {
      const bruto = sessionStorage.getItem(CHAVE);
      const lido: unknown = bruto ? JSON.parse(bruto) : [];
      return Array.isArray(lido) ? lido.filter((item): item is string => typeof item === 'string') : [];
    } catch {
      return [];
    }
  }

  private gravar(): void {
    try {
      sessionStorage.setItem(CHAVE, JSON.stringify([...this.vistas]));
    } catch {
      return;
    }
  }
}
