import { ChangeDetectionStrategy, Component, Injector, afterNextRender, computed, effect, inject, input, signal, untracked, viewChildren } from '@angular/core';
import { Router } from '@angular/router';
import { casoDe, mensagemDoErro } from '../../../../../core/erro';
import { ROTA_DE_ENTRADA } from '../../../../../core/navegacao/destino-interno';
import { TokenDaSessao } from '../../../../../core/sessao';
import { reais, reaisSemQuebra } from '../../../../../shared/formatacao/reais';
import { AvisoService } from '../../../../../shared/ui/aviso/aviso.service';
import { DialogoDeAlvoComponent, PedidoDeAlvo } from '../../../../../shared/ui/dialogo/dialogo-de-alvo.component';
import { CardExternoComponent } from '../../../components/card-externo/card-externo.component';
import { MonitoramentoDoExterno } from '../../../components/produto-externo-exibido';
import { MonitoramentoDoProdutoService } from '../../../data/monitoramento-do-produto.service';
import { ProdutoExterno } from '../../../data/produto-externo';
import { RascunhoDoAlvo, rascunhoDoJogoExterno } from '../../../data/rascunho-do-alvo';

interface PedidoDeMonitoramento {
  readonly produto: ProdutoExterno;
  readonly alvoCentavos: number | null;
  readonly retomado: boolean;
}

@Component({
  selector: 'umbra-fora-do-catalogo',
  imports: [CardExternoComponent, DialogoDeAlvoComponent],
  templateUrl: './fora-do-catalogo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForaDoCatalogoComponent {
  private readonly monitoramentos = inject(MonitoramentoDoProdutoService);
  private readonly rascunho = inject(RascunhoDoAlvo);
  private readonly sessao = inject(TokenDaSessao);
  private readonly avisos = inject(AvisoService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly cards = viewChildren(CardExternoComponent);

  readonly produtos = input.required<readonly ProdutoExterno[]>();

  protected readonly estados = signal<ReadonlyMap<string, MonitoramentoDoExterno>>(new Map());
  protected readonly pedido = signal<PedidoDeMonitoramento | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly anuncio = signal('');

  protected readonly pedidoDeAlvo = computed<PedidoDeAlvo | null>(() => {
    const pedido = this.pedido();
    if (pedido === null) {
      return null;
    }
    const { produto } = pedido;
    return {
      titulo: produto.nome,
      apoio: `Melhor preço ${reaisSemQuebra(produto.precoCentavos)} na ${produto.preco.loja}`,
      rotuloDoCampo: 'Preço alvo',
      alvoCentavos: pedido.alvoCentavos,
      acao: 'Monitorar',
      acaoEmAndamento: 'Incluindo…',
      focarAcao: pedido.retomado,
    };
  });

  constructor() {
    effect(() => {
      const produtos = this.produtos();
      untracked(() => this.retomarRascunho(produtos));
    });
  }

  protected monitorar(produto: ProdutoExterno): void {
    this.erro.set(null);
    this.pedido.set({ produto, alvoCentavos: null, retomado: false });
  }

  protected cancelar(): void {
    const pedido = this.pedido();
    this.pedido.set(null);
    if (pedido !== null) {
      this.focarDepois(pedido.produto.gid, (card) => card.focarMonitorar());
    }
  }

  protected async salvar(alvoCentavos: number): Promise<void> {
    const pedido = this.pedido();
    if (pedido === null) {
      return;
    }
    if (this.sessao.vigente() === null) {
      this.rascunho.guardar(rascunhoDoJogoExterno(pedido.produto.gid), alvoCentavos);
      await this.router.navigate([ROTA_DE_ENTRADA], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    await this.incluir(pedido.produto, alvoCentavos);
  }

  private async incluir(produto: ProdutoExterno, alvoCentavos: number): Promise<void> {
    this.enviando.set(true);
    this.erro.set(null);
    try {
      const criado = await this.monitoramentos.criarPorGid(produto.gid, alvoCentavos);
      this.concluir(produto, {
        tipo: 'monitorando',
        produtoId: criado.produtoId,
        alvo: reais(criado.precoAlvoCentavos),
      });
      this.anuncio.set(`${produto.nome} monitorado`);
      this.avisos.mostrar(`Pronto. Agora eu vigio ${produto.nome} para você.`);
    } catch (falha) {
      this.tratarFalha(produto, falha);
    } finally {
      this.enviando.set(false);
    }
  }

  private tratarFalha(produto: ProdutoExterno, falha: unknown): void {
    if (casoDe(falha).tipo === 'conflito') {
      this.concluir(produto, { tipo: 'ja-monitorado' });
      return;
    }
    const mensagem = mensagemDoErro(falha);
    if (mensagem === null) {
      this.pedido.set(null);
      return;
    }
    this.erro.set(mensagem);
  }

  private concluir(produto: ProdutoExterno, estado: MonitoramentoDoExterno): void {
    this.estados.update((atuais) => new Map(atuais).set(produto.gid, estado));
    this.pedido.set(null);
    this.focarDepois(produto.gid, (card) => card.focarMonitoramento());
  }

  private retomarRascunho(produtos: readonly ProdutoExterno[]): void {
    if (this.sessao.vigente() === null) {
      return;
    }
    for (const produto of produtos) {
      const alvoCentavos = this.rascunho.retomar(rascunhoDoJogoExterno(produto.gid));
      if (alvoCentavos !== null) {
        this.erro.set(null);
        this.pedido.set({ produto, alvoCentavos, retomado: true });
        return;
      }
    }
  }

  private focarDepois(gid: string, foco: (card: CardExternoComponent) => void): void {
    afterNextRender(
      () => {
        const card = this.cards()[this.produtos().findIndex((produto) => produto.gid === gid)];
        if (card !== undefined) {
          foco(card);
        }
      },
      { injector: this.injector },
    );
  }
}
