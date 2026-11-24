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
import { RodizioDePiadas, faixaDoAlvo } from './piadas-do-alvo';
import { LeituraDoAlvo, lerAlvo, textoDoAlvo } from '../../../../../shared/formatacao/preco-alvo';
import { RascunhoDoAlvo } from './rascunho-do-alvo';

export type SituacaoDoMonitoramento =
  | { readonly tipo: 'deslogado' }
  | { readonly tipo: 'conferindo' }
  | { readonly tipo: 'livre' }
  | { readonly tipo: 'monitorado'; readonly monitoramento: MonitoramentoDoProduto };

const ROTA_DOS_MONITORAMENTOS = '/monitoramentos';

const MENSAGEM_DO_ALVO: Readonly<Record<Exclude<LeituraDoAlvo['tipo'], 'valido'>, string>> = {
  vazio: 'Informe o preço alvo.',
  invalido: 'Use só números, como 149,90 ou 149.90.',
  zero: 'O alvo precisa ser maior que zero.',
  'acima-do-maximo': 'O alvo vai até R$ 150.000,00.',
};

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
  private readonly piadas = inject(RodizioDePiadas);

  readonly produtoId = input.required<number>();
  readonly gratuito = input.required<boolean>();
  readonly situacao = input.required<SituacaoDoMonitoramento>();
  readonly mudou = output<MonitoramentoDoProduto | null>();

  private readonly botao = viewChild<ElementRef<HTMLButtonElement>>('botao');
  private readonly campo = viewChild<ElementRef<HTMLInputElement>>('campo');

  protected readonly texto = signal('');
  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);
  protected readonly editando = signal(false);
  protected readonly alvoAtual = computed(() => {
    const situacao = this.situacao();
    return situacao.tipo === 'monitorado' ? reais(situacao.monitoramento.precoAlvoCentavos) : null;
  });

  private focarAoAparecer = false;
  private focarCampoAoAparecer = false;

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
      const campo = this.campo();
      if (this.focarCampoAoAparecer && campo) {
        this.focarCampoAoAparecer = false;
        campo.nativeElement.focus();
        campo.nativeElement.select();
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

  protected readonly modo = computed(() => {
    const tipo = this.situacao().tipo;
    return tipo === 'monitorado' && this.editando() ? 'editando' : tipo;
  });

  protected editarAtual(): void {
    const situacao = this.situacao();
    if (situacao.tipo === 'monitorado') {
      this.iniciarEdicao(situacao.monitoramento);
    }
  }

  private iniciarEdicao(monitoramento: MonitoramentoDoProduto): void {
    this.texto.set(textoDoAlvo(monitoramento.precoAlvoCentavos));
    this.erro.set(null);
    this.editando.set(true);
    this.focarCampoAoAparecer = true;
  }

  protected cancelarEdicao(): void {
    this.editando.set(false);
    this.erro.set(null);
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
    const leitura = lerAlvo(this.texto());
    if (leitura.tipo !== 'valido') {
      this.erro.set(MENSAGEM_DO_ALVO[leitura.tipo]);
      return;
    }
    const alvo = leitura.centavos;
    if (this.sessao.vigente() === null) {
      this.rascunho.guardar(this.produtoId(), alvo);
      await this.router.navigate([ROTA_DE_ENTRADA], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    const situacao = this.situacao();
    if (this.editando() && situacao.tipo === 'monitorado') {
      await this.alterar(situacao.monitoramento.id, alvo);
      return;
    }
    await this.criar(alvo);
  }

  private avisar(alvo: number, resumo: string, textoComum: string): void {
    const faixa = faixaDoAlvo(alvo);
    if (faixa !== null) {
      const piada = this.piadas.proxima(faixa);
      this.avisos.mostrarLongo({ texto: piada.texto, detalhe: resumo, figura: piada.figura });
      return;
    }
    this.avisos.mostrar(textoComum);
  }

  private async alterar(id: number, alvo: number): Promise<void> {
    this.salvando.set(true);
    try {
      const monitoramento = await this.monitoramentos.alterarAlvo(id, alvo);
      this.avisar(alvo, 'Alvo atualizado.', 'Alvo atualizado. O Umbra segue de olho.');
      this.editando.set(false);
      this.mudou.emit(monitoramento);
    } catch (falha) {
      const caso = casoDe(falha);
      if (caso.tipo === 'nao-encontrado') {
        this.avisos.mostrar('Esse monitoramento não existe mais. Crie de novo.', 'confuso');
        this.editando.set(false);
        this.mudou.emit(null);
        return;
      }
      this.erro.set(caso.tipo === 'invalido' ? MENSAGEM_DO_ALVO.invalido : mensagemDoCaso(caso));
    } finally {
      this.salvando.set(false);
    }
  }

  private async criar(alvo: number): Promise<void> {
    this.salvando.set(true);
    try {
      const monitoramento = await this.monitoramentos.criar(this.produtoId(), alvo);
      this.avisar(alvo, 'Alvo salvo.', 'Monitoramento criado. O Umbra avisa quando o preço chegar lá.');
      this.mudou.emit(monitoramento);
    } catch (falha) {
      const caso = casoDe(falha);
      if (caso.tipo === 'conflito') {
        this.avisos.mostrar('Você já monitora este produto.', 'tranquilo');
        await this.router.navigateByUrl(ROTA_DOS_MONITORAMENTOS);
        return;
      }
      this.erro.set(caso.tipo === 'invalido' ? MENSAGEM_DO_ALVO.invalido : mensagemDoCaso(caso));
    } finally {
      this.salvando.set(false);
    }
  }
}
