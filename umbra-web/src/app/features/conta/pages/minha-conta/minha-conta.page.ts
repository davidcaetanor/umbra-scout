import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DadosDaConta } from '../../data/dados-da-conta';
import { ContaService } from '../../data/conta.service';

type EstadoDaConta =
  | { readonly tipo: 'carregando' }
  | { readonly tipo: 'erro' }
  | { readonly tipo: 'pronto'; readonly dados: DadosDaConta };

@Component({
  selector: 'umbra-minha-conta-page',
  templateUrl: './minha-conta.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MinhaContaPage implements OnInit {
  private readonly conta = inject(ContaService);

  protected readonly estado = signal<EstadoDaConta>({ tipo: 'carregando' });
  protected readonly dados = computed(() => {
    const atual = this.estado();
    return atual.tipo === 'pronto' ? atual.dados : null;
  });

  ngOnInit(): void {
    void this.carregar();
  }

  protected async carregar(): Promise<void> {
    this.estado.set({ tipo: 'carregando' });
    try {
      this.estado.set({ tipo: 'pronto', dados: await this.conta.carregar() });
    } catch {
      this.estado.set({ tipo: 'erro' });
    }
  }
}
