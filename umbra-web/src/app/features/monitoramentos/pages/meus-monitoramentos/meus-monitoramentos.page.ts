import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  linkedSignal,
  resource,
  signal,
  viewChild,
  viewChildren,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { carregarEstado, mensagemDoErro } from '../../../../core/erro';
import { RELOGIO } from '../../../../core/sessao/relogio';
import { reais } from '../../../../shared/formatacao/reais';
import { BalaoFalaComponent } from '../../../../shared/mascote/balao-fala.component';
import { EntradasVistas } from '../../../../shared/mascote/entradas-vistas';
import { MascoteDestaqueComponent } from '../../../../shared/mascote/mascote-destaque.component';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';
import { AvisoService } from '../../../../shared/ui/aviso/aviso.service';
import { EstadoDoBloco } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco';
import { EstadoDoBlocoComponent } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco.component';
import { AlteracaoPedida, AlterarAlvoComponent } from '../../components/alterar-alvo/alterar-alvo.component';
import { ConfirmarRemocaoComponent } from '../../components/confirmar-remocao/confirmar-remocao.component';
import { ItemMonitoramentoComponent } from '../../components/item-monitoramento/item-monitoramento.component';
import { atingidosPrimeiro, chaveDaNovidade, contagem, diaLocal, fraseDaRonda, todosPausados } from '../../components/leitura-da-lista';
import { MeusMonitoramentosService } from '../../data/meus-monitoramentos.service';
import { MonitoramentoListado } from '../../data/monitoramento-listado';

export const CHAVE_DA_RONDA = 'umbra.monitoramentos.ronda';

type FiguraDoTopo = 'pausado' | 'novidade' | 'tranquilo';

export interface NovidadeExibida {
  readonly nome: string;
  readonly preco: string;
  readonly loja: string;
  readonly outras: number;
}

@Component({
  selector: 'umbra-meus-monitoramentos-page',
  imports: [
    AlterarAlvoComponent,
    BalaoFalaComponent,
    ConfirmarRemocaoComponent,
    EstadoDoBlocoComponent,
    ItemMonitoramentoComponent,
    MascoteComponent,
    MascoteDestaqueComponent,
    NgTemplateOutlet,
    RouterLink,
  ],
  templateUrl: './meus-monitoramentos.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeusMonitoramentosPage {
  private readonly servico = inject(MeusMonitoramentosService);
  private readonly avisos = inject(AvisoService);
  private readonly entradas = inject(EntradasVistas);
  private readonly documento = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private readonly agora = inject(RELOGIO);
  private readonly itensNaTela = viewChildren(ItemMonitoramentoComponent);
  private readonly tituloDaPagina = viewChild<ElementRef<HTMLElement>>('tituloDaPagina');

  private readonly recurso = resource({ loader: () => carregarEstado(() => this.servico.listarTodos()) });

  protected readonly itens = linkedSignal<EstadoDoBloco<MonitoramentoListado[]> | undefined, MonitoramentoListado[] | null>({
    source: () => this.recurso.value(),
    computation: (estado) => (estado?.tipo === 'sucesso' ? estado.dados : null),
  });

  protected readonly estado = computed<EstadoDoBloco<MonitoramentoListado[]>>(() => {
    const itens = this.itens();
    if (itens !== null) {
      return { tipo: 'sucesso', dados: itens };
    }
    const carregado = this.recurso.value();
    return carregado?.tipo === 'erro' ? carregado : { tipo: 'carregando' };
  });

  protected readonly itensEmOrdem = computed(() => atingidosPrimeiro(this.itens() ?? []));
  protected readonly vazio = computed(() => this.itens()?.length === 0);
  protected readonly contagem = computed(() => contagem(this.itens() ?? []));
  protected readonly todosPausados = computed(() => todosPausados(this.itens() ?? []));

  private readonly idsDaNovidade = linkedSignal<MonitoramentoListado[] | null, readonly number[]>({
    source: () => (this.recurso.value()?.tipo === 'sucesso' ? this.itens() : null),
    computation: (itens, anterior) => {
      if (anterior !== undefined && anterior.source !== null) {
        return anterior.value;
      }
      return itens === null ? [] : this.registrarNovidades(itens);
    },
  });

  protected readonly novidade = computed<NovidadeExibida | null>(() => {
    const ids = this.idsDaNovidade();
    const atingidos = (this.itens() ?? []).filter((item) => item.alvoAtingido && ids.includes(item.id));
    const [primeiro] = atingidos;
    if (primeiro?.melhorPreco == null) {
      return null;
    }
    return {
      nome: primeiro.nome,
      preco: reais(primeiro.melhorPreco.valorCentavos),
      loja: primeiro.melhorPreco.loja,
      outras: atingidos.length - 1,
    };
  });

  protected readonly fraseDaRonda = signal(this.rondaDoDia());

  protected readonly figura = computed<FiguraDoTopo>(() => {
    if (this.todosPausados()) {
      return 'pausado';
    }
    return this.novidade() === null ? 'tranquilo' : 'novidade';
  });
  protected readonly temFala = computed(
    () => this.novidade() !== null || this.todosPausados() || this.fraseDaRonda() !== null,
  );
  protected readonly anuncio = signal('');
  protected readonly ocupados = signal<ReadonlySet<number>>(new Set());
  protected readonly pedidoDeRemocao = signal<MonitoramentoListado | null>(null);
  protected readonly removendo = signal(false);
  protected readonly pedidoDeAlteracao = signal<MonitoramentoListado | null>(null);
  protected readonly alteracaoPedida = computed<AlteracaoPedida | null>(() => {
    const item = this.pedidoDeAlteracao();
    return item === null
      ? null
      : { nome: item.nome, precoAlvoCentavos: item.precoAlvoCentavos, melhorPrecoCentavos: item.melhorPreco?.valorCentavos ?? null };
  });
  protected readonly salvandoAlvo = signal(false);
  protected readonly erroDoAlvo = signal<string | null>(null);

  protected tentarDeNovo(): void {
    this.recurso.reload();
  }

  protected fecharNovidade(): void {
    this.idsDaNovidade.set([]);
  }

  protected fecharRonda(): void {
    this.fraseDaRonda.set(null);
  }

  protected async alternarAtivo(item: MonitoramentoListado): Promise<void> {
    this.marcarOcupado(item.id, true);
    try {
      const atualizado = await this.servico.definirAtivo(item.id, !item.ativo);
      this.itens.update((itens) => itens?.map((atual) => (atual.id === item.id ? atualizado : atual)) ?? null);
      this.anuncio.set(`${item.nome} ${atualizado.ativo ? 'reativado' : 'pausado'}`);
    } catch (erro) {
      this.avisarFalha(erro);
    } finally {
      this.marcarOcupado(item.id, false);
    }
  }

  protected pedirAlteracao(item: MonitoramentoListado): void {
    this.erroDoAlvo.set(null);
    this.pedidoDeAlteracao.set(item);
  }

  protected cancelarAlteracao(): void {
    const item = this.pedidoDeAlteracao();
    this.pedidoDeAlteracao.set(null);
    if (item !== null) {
      this.focarDepois(() => this.itemNaTela(item.id)?.focarAlteracao());
    }
  }

  protected async salvarAlvo(precoAlvoCentavos: number): Promise<void> {
    const item = this.pedidoDeAlteracao();
    if (item === null) {
      return;
    }
    this.salvandoAlvo.set(true);
    this.erroDoAlvo.set(null);
    try {
      const atualizado = await this.servico.alterarAlvo(item.id, precoAlvoCentavos);
      this.itens.update((itens) => itens?.map((atual) => (atual.id === item.id ? atualizado : atual)) ?? null);
      this.pedidoDeAlteracao.set(null);
      this.anuncio.set(`Alvo de ${item.nome} alterado para ${reais(atualizado.precoAlvoCentavos)}`);
      this.focarDepois(() => this.itemNaTela(item.id)?.focarAlteracao());
    } catch (erro) {
      const mensagem = mensagemDoErro(erro);
      if (mensagem === null) {
        this.pedidoDeAlteracao.set(null);
      }
      this.erroDoAlvo.set(mensagem);
    } finally {
      this.salvandoAlvo.set(false);
    }
  }

  protected pedirRemocao(item: MonitoramentoListado): void {
    this.pedidoDeRemocao.set(item);
  }

  protected cancelarRemocao(): void {
    const item = this.pedidoDeRemocao();
    this.pedidoDeRemocao.set(null);
    if (item !== null) {
      this.focarDepois(() => this.itemNaTela(item.id)?.focarRemocao());
    }
  }

  protected async confirmarRemocao(): Promise<void> {
    const item = this.pedidoDeRemocao();
    if (item === null) {
      return;
    }
    this.removendo.set(true);
    try {
      await this.servico.remover(item.id);
      const itens = this.itensEmOrdem();
      const posicao = itens.findIndex((atual) => atual.id === item.id);
      const restantes = itens.filter((atual) => atual.id !== item.id);
      this.itens.set(restantes);
      this.pedidoDeRemocao.set(null);
      this.anuncio.set(`${item.nome} removido`);
      const proximo = restantes[Math.min(posicao, restantes.length - 1)];
      this.focarDepois(() =>
        proximo === undefined ? this.tituloDaPagina()?.nativeElement.focus() : this.itemNaTela(proximo.id)?.focarTitulo(),
      );
    } catch (erro) {
      this.pedidoDeRemocao.set(null);
      this.avisarFalha(erro);
      this.focarDepois(() => this.itemNaTela(item.id)?.focarRemocao());
    } finally {
      this.removendo.set(false);
    }
  }

  protected estaOcupado(id: number): boolean {
    return this.ocupados().has(id);
  }

  private registrarNovidades(itens: readonly MonitoramentoListado[]): readonly number[] {
    const atingidos = itens.filter((item) => item.alvoAtingido && !this.entradas.jaViu(chaveDaNovidade(item)));
    atingidos.forEach((item) => this.entradas.registrar(chaveDaNovidade(item)));
    return atingidos.map((item) => item.id);
  }

  private rondaDoDia(): string | null {
    const agora = this.agora();
    const hoje = diaLocal(agora);
    try {
      const armazenamento = this.documento.defaultView?.localStorage;
      if (armazenamento === undefined || armazenamento.getItem(CHAVE_DA_RONDA) === hoje) {
        return null;
      }
      armazenamento.setItem(CHAVE_DA_RONDA, hoje);
    } catch {
      return null;
    }
    return fraseDaRonda(agora);
  }

  private marcarOcupado(id: number, ocupado: boolean): void {
    this.ocupados.update((atuais) => {
      const novos = new Set(atuais);
      if (ocupado) {
        novos.add(id);
      } else {
        novos.delete(id);
      }
      return novos;
    });
  }

  private avisarFalha(erro: unknown): void {
    const mensagem = mensagemDoErro(erro);
    if (mensagem !== null) {
      this.avisos.mostrar(mensagem, 'confuso');
    }
  }

  private itemNaTela(id: number): ItemMonitoramentoComponent | undefined {
    return this.itensNaTela().find((componente) => componente.item().id === id);
  }

  private focarDepois(foco: () => void): void {
    afterNextRender(foco, { injector: this.injector });
  }
}
