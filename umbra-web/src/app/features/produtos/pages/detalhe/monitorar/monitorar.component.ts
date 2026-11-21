import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  afterRenderEffect,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { casoDe, mensagemDoCaso } from '../../../../../core/erro';
import { ROTA_DE_ENTRADA } from '../../../../../core/navegacao/destino-interno';
import { TokenDaSessao } from '../../../../../core/sessao';
import { reais } from '../../../../../shared/formatacao/reais';
import { MascoteComponent } from '../../../../../shared/mascote/mascote.component';
import { AvisoService } from '../../../../../shared/ui/aviso/aviso.service';
import { MonitoramentoDoProduto } from '../../../data/monitoramento-do-produto';
import { MonitoramentoDoProdutoService } from '../../../data/monitoramento-do-produto.service';
import { centavosDoAlvo, textoDoAlvo } from './preco-alvo';
import { RascunhoDoAlvo } from './rascunho-do-alvo';

export type SituacaoDoMonitoramento =
  | { readonly tipo: 'deslogado' }
  | { readonly tipo: 'conferindo' }
  | { readonly tipo: 'livre' }
  | { readonly tipo: 'monitorado'; readonly monitoramento: MonitoramentoDoProduto };

const ROTA_DOS_MONITORAMENTOS = '/monitoramentos';

@Component({
  selector: 'umbra-monitorar',
  imports: [MascoteComponent, RouterLink],
  templateUrl: './monitorar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class MonitorarComponent {
  private readonly router = inject(Router);
  private readonly sessao = inject(TokenDaSessao);
  private readonly monitoramentos = inject(MonitoramentoDoProdutoService);
  private readonly avisos = inject(AvisoService);
  private readonly rascunho = inject(RascunhoDoAlvo);

  readonly produtoId = input.required<number>();
  readonly gratuito = input.required<boolean>();
  readonly situacao = input.required<SituacaoDoMonitoramento>();
  readonly criado = output<MonitoramentoDoProduto>();

  private readonly botao = viewChild<ElementRef<HTMLButtonElement>>('botao');

  protected readonly texto = signal('');
  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);
  protected readonly alvoAtual = computed(() => {
    const situacao = this.situacao();
    return situacao.tipo === 'monitorado' ? reais(situacao.monitoramento.precoAlvoCentavos) : null;
  });

  private focarAoAparecer = false;

  constructor() {
    if (this.sessao.vigente() !== null) {
      afterNextRender(() => this.retomarRascunho());
    }
    afterRenderEffect(() => {
      const botao = this.botao();
      if (this.focarAoAparecer && botao) {
        this.focarAoAparecer = false;
        botao.nativeElement.focus();
      }
    });
  }

  private retomarRascunho(): void {
    const retomado = this.rascunho.retomar(this.produtoId());
    if (retomado !== null) {
      this.texto.set(textoDoAlvo(retomado));
      this.focarAoAparecer = true;
    }
  }

  protected digitar(evento: Event): void {
    this.texto.set((evento.target as HTMLInputElement).value);
    this.erro.set(null);
  }

  protected async enviar(evento: Event): Promise<void> {
    evento.preventDefault();
    if (this.salvando()) {
      return;
    }
    const alvo = centavosDoAlvo(this.texto());
    if (alvo === null) {
      this.erro.set(this.texto().trim() === '' ? 'Informe o preço alvo.' : 'Use um valor em reais, como 149,90.');
      return;
    }
    if (this.sessao.vigente() === null) {
      this.rascunho.guardar(this.produtoId(), alvo);
      await this.router.navigate([ROTA_DE_ENTRADA], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    await this.criar(alvo);
  }

  private async criar(alvo: number): Promise<void> {
    this.salvando.set(true);
    try {
      const monitoramento = await this.monitoramentos.criar(this.produtoId(), alvo);
      this.avisos.mostrar('Monitoramento criado. O Umbra avisa quando o preço chegar lá.');
      this.criado.emit(monitoramento);
    } catch (falha) {
      const caso = casoDe(falha);
      if (caso.tipo === 'conflito') {
        this.avisos.mostrar('Você já monitora este produto.', 'tranquilo');
        await this.router.navigateByUrl(ROTA_DOS_MONITORAMENTOS);
        return;
      }
      this.erro.set(caso.tipo === 'invalido' ? 'Use um valor em reais, como 149,90.' : mensagemDoCaso(caso));
    } finally {
      this.salvando.set(false);
    }
  }
}
