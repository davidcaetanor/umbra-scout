import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { DESTINO_PADRAO } from '../../navegacao/destino-interno';
import { ITENS_NAVEGACAO } from '../../navegacao/itens-navegacao';
import { SessaoService } from '../../sessao';

@Component({
  selector: 'umbra-cabecalho',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './cabecalho.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'fecharSeForaDoMenu($event)',
    '(document:keydown.escape)': 'fecharMenu(true)',
  },
})
export class CabecalhoComponent {
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly conta = viewChild<ElementRef<HTMLElement>>('conta');
  private readonly botaoDaConta = viewChild<ElementRef<HTMLButtonElement>>('botaoDaConta');

  protected readonly itens = ITENS_NAVEGACAO;
  protected readonly autenticado = this.sessao.autenticado;
  protected readonly usuario = this.sessao.usuario;
  protected readonly nomeDaConta = computed(() => this.usuario()?.nome ?? 'Conta');
  protected readonly menuAberto = signal(false);

  protected alternarMenu(): void {
    this.menuAberto.update((aberto) => !aberto);
  }

  protected fecharMenu(devolverFoco = false): void {
    if (!this.menuAberto()) {
      return;
    }
    this.menuAberto.set(false);
    if (devolverFoco) {
      this.botaoDaConta()?.nativeElement.focus();
    }
  }

  protected fecharSeForaDoMenu(evento: Event): void {
    if (!this.conta()?.nativeElement.contains(evento.target as Node)) {
      this.fecharMenu();
    }
  }

  protected async sair(): Promise<void> {
    this.menuAberto.set(false);
    this.sessao.sair();
    await this.router.navigateByUrl(DESTINO_PADRAO);
  }
}
