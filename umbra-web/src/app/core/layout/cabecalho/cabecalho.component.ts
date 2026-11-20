import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map } from 'rxjs';
import { DESTINO_PADRAO } from '../../navegacao/destino-interno';
import { ITENS_NAVEGACAO } from '../../navegacao/itens-navegacao';
import { SessaoService } from '../../sessao';

const ROTAS_SEM_BUSCA_NO_CABECALHO = ['/', '/catalogo', '/entrar', '/cadastro', '/login/callback'];
const TAMANHO_MINIMO_DO_TERMO = 3;

function caminhoDe(url: string): string {
  return url.split(/[?#]/)[0] || '/';
}

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
  protected readonly nomeAcessivel = computed(() => `Conta de ${this.usuario()?.nomeCompleto ?? 'usuário'}`);
  protected readonly menuAberto = signal(false);

  private readonly caminho = toSignal(
    this.router.events.pipe(
      filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
      map((evento) => caminhoDe(evento.urlAfterRedirects)),
    ),
    { initialValue: caminhoDe(this.router.url) },
  );
  protected readonly mostrarBusca = computed(() => !ROTAS_SEM_BUSCA_NO_CABECALHO.includes(this.caminho()));

  protected async buscar(evento: Event, campo: HTMLInputElement): Promise<void> {
    evento.preventDefault();
    const termo = campo.value.trim();
    if (termo.length < TAMANHO_MINIMO_DO_TERMO) {
      campo.reportValidity();
      return;
    }
    campo.value = '';
    await this.router.navigate(['/catalogo'], { queryParams: { nome: termo } });
  }

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
