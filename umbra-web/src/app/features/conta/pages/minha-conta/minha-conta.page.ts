import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { carregarEstado } from '../../../../core/erro';
import { EstadoDoBloco, dadosDe } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco';
import { EstadoDoBlocoComponent } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco.component';
import { DadosDaConta } from '../../data/dados-da-conta';
import { ContaService } from '../../data/conta.service';

@Component({
  selector: 'umbra-minha-conta-page',
  imports: [EstadoDoBlocoComponent],
  templateUrl: './minha-conta.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MinhaContaPage implements OnInit {
  private readonly conta = inject(ContaService);

  protected readonly estado = signal<EstadoDoBloco<DadosDaConta>>({ tipo: 'carregando' });
  protected readonly dados = computed(() => dadosDe(this.estado()));

  ngOnInit(): void {
    void this.carregar();
  }

  protected async carregar(): Promise<void> {
    this.estado.set({ tipo: 'carregando' });
    this.estado.set(await carregarEstado(() => this.conta.carregar()));
  }
}
