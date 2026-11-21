import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, resource, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { carregarEstado, casoDe, mensagemDoErro } from '../../../../core/erro';
import { PaginaNaoEncontradaPage } from '../../../../core/erro/pagina-nao-encontrada/pagina-nao-encontrada.page';
import { TokenDaSessao } from '../../../../core/sessao';
import { diaMesEAno, hora } from '../../../../shared/formatacao/data-curta';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';
import { EstadoDoBloco, dadosDe } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco';
import { EstadoDoBlocoComponent } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco.component';
import { GraficoHistoricoComponent } from '../../components/grafico-historico/grafico-historico.component';
import { LegendaHistoricoComponent } from '../../components/legenda-historico/legenda-historico.component';
import { PrecoProdutoComponent } from '../../components/preco-produto/preco-produto.component';
import { OfertaExibida } from '../../components/tabela-lojas/oferta-exibida';
import { TabelaLojasComponent } from '../../components/tabela-lojas/tabela-lojas.component';
import { TopoExibido } from '../../components/topo-produto/topo-exibido';
import { TopoHardwareComponent } from '../../components/topo-produto/topo-hardware.component';
import { TopoJogoComponent } from '../../components/topo-produto/topo-jogo.component';
import { HistoricoDoProduto, PeriodoDoHistorico } from '../../data/historico-do-produto';
import { MonitoramentoDoProduto } from '../../data/monitoramento-do-produto';
import { MonitoramentoDoProdutoService } from '../../data/monitoramento-do-produto.service';
import { ProdutoDetalhado } from '../../data/produto-detalhado';
import { ProdutoService } from '../../data/produto.service';
import {
  ContextoDoGrafico,
  MAXIMO_DE_LOJAS_NO_GRAFICO,
  diasObservados,
  graficoExibido,
  legendaExibida,
  lojasIniciais,
  opcoesDePeriodo,
  periodoEfetivo,
  temHistorico,
} from './historico-exibido';
import { leituraDoPreco } from './leitura-do-preco';
import { MonitorarComponent, SituacaoDoMonitoramento } from './monitorar/monitorar.component';

const ID_VALIDO = /^[1-9]\d{0,14}$/;
const MARCA = 'Umbra';
const TITULO_NAO_ENCONTRADO = 'Página não encontrada';

@Component({
  selector: 'umbra-detalhe-page',
  imports: [
    EstadoDoBlocoComponent,
    GraficoHistoricoComponent,
    LegendaHistoricoComponent,
    MascoteComponent,
    MonitorarComponent,
    NgTemplateOutlet,
    PaginaNaoEncontradaPage,
    PrecoProdutoComponent,
    TabelaLojasComponent,
    TopoHardwareComponent,
    TopoJogoComponent,
  ],
  templateUrl: './detalhe.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetalhePage {
  private readonly produtos = inject(ProdutoService);
  private readonly monitoramentos = inject(MonitoramentoDoProdutoService);
  private readonly sessao = inject(TokenDaSessao);
  private readonly titulo = inject(Title);

  readonly id = input.required<string>();

  protected readonly produtoId = computed(() => (ID_VALIDO.test(this.id()) ? Number(this.id()) : null));

  private readonly recursoDoProduto = resource({
    params: () => this.produtoId() ?? undefined,
    loader: ({ params }) => estadoDoProduto(() => this.produtos.detalhar(params)),
  });

  protected readonly estadoDoProduto = computed<EstadoDoBloco<ProdutoDetalhado>>(() =>
    this.produtoId() === null ? { tipo: 'vazio' } : (this.recursoDoProduto.value() ?? { tipo: 'carregando' }),
  );
  protected readonly produto = computed(() => dadosDe(this.estadoDoProduto()));

  protected readonly periodoEscolhido = signal<PeriodoDoHistorico>('90d');

  private readonly recursoDoHistorico = resource({
    params: () => {
      const id = this.produtoId();
      return id === null ? undefined : { id, periodo: this.periodoEscolhido() };
    },
    loader: ({ params }) => carregarEstado(() => this.produtos.historico(params.id, params.periodo)),
  });

  protected readonly estadoDoHistorico = linkedSignal<EstadoDoBloco<HistoricoDoProduto> | undefined, EstadoDoBloco<HistoricoDoProduto>>({
    source: () => this.recursoDoHistorico.value(),
    computation: (novo, anterior) => novo ?? anterior?.value ?? { tipo: 'carregando' },
  });
  protected readonly atualizandoHistorico = computed(() => this.recursoDoHistorico.isLoading());
  protected readonly historico = computed(() => dadosDe(this.estadoDoHistorico()));

  protected readonly opcoesDePeriodo = computed(() => {
    const historico = this.historico();
    return opcoesDePeriodo(historico === null ? 0 : diasObservados(historico));
  });
  protected readonly periodosIndisponiveis = computed(() => this.opcoesDePeriodo().filter((opcao) => !opcao.disponivel));
  protected readonly periodo = computed(() => periodoEfetivo(this.periodoEscolhido(), this.opcoesDePeriodo()));
  protected readonly comHistorico = computed(() => {
    const historico = this.historico();
    return historico !== null && temHistorico(historico);
  });
  protected readonly frasesDoVazio = computed(() => {
    const historico = this.historico();
    const dias = historico === null ? 0 : diasObservados(historico);
    if (historico?.observadoDesde == null) {
      return 'O Umbra começou a observar este produto agora.';
    }
    return `O Umbra observa este produto há ${dias} ${dias === 1 ? 'dia' : 'dias'}.`;
  });

  protected readonly porLoja = signal(false);
  protected readonly lojasEscolhidas = linkedSignal<HistoricoDoProduto | null, readonly string[]>({
    source: this.historico,
    computation: (historico, anterior) => {
      if (historico === null) {
        return anterior?.value ?? [];
      }
      const mantidas = (anterior?.value ?? []).filter((loja) => historico.porLoja.some((serie) => serie.loja === loja));
      return mantidas.length > 0 ? mantidas : lojasIniciais(historico);
    },
  });
  protected readonly lojasDoHistorico = computed(() => this.historico()?.porLoja.map((serie) => serie.loja) ?? []);
  protected readonly maximoDeLojas = MAXIMO_DE_LOJAS_NO_GRAFICO;

  private readonly recursoDoMonitoramento = resource({
    params: () => (this.sessao.autenticado() ? (this.produtoId() ?? undefined) : undefined),
    loader: ({ params }) => carregarEstado(() => this.monitoramentos.buscar(params)),
  });

  protected readonly situacaoDoMonitoramento = computed<SituacaoDoMonitoramento>(() => {
    if (!this.sessao.autenticado()) {
      return { tipo: 'deslogado' };
    }
    const estado = this.recursoDoMonitoramento.value();
    if (estado === undefined || estado.tipo === 'carregando') {
      return { tipo: 'conferindo' };
    }
    return estado.tipo === 'sucesso' && estado.dados !== null ? { tipo: 'monitorado', monitoramento: estado.dados } : { tipo: 'livre' };
  });
  private readonly alvoCentavos = computed(() => {
    const situacao = this.situacaoDoMonitoramento();
    return situacao.tipo === 'monitorado' ? situacao.monitoramento.precoAlvoCentavos : null;
  });

  private readonly contexto = computed<ContextoDoGrafico | null>(() => {
    const historico = this.historico();
    const produto = this.produto();
    if (historico === null || produto === null) {
      return null;
    }
    return {
      historico,
      periodo: this.periodo(),
      melhorOferta: produto.melhorOferta,
      ofertas: produto.ofertas,
      alvoCentavos: this.alvoCentavos(),
      lojas: this.porLoja() ? this.lojasEscolhidas() : null,
    };
  });
  protected readonly grafico = computed(() => {
    const contexto = this.contexto();
    return contexto === null ? null : graficoExibido(contexto);
  });
  protected readonly legenda = computed(() => {
    const contexto = this.contexto();
    return contexto === null ? [] : legendaExibida(contexto);
  });

  protected readonly ofertas = computed<readonly OfertaExibida[]>(() => {
    const produto = this.produto();
    if (produto === null) {
      return [];
    }
    return produto.ofertas.map((oferta) => ({
      loja: oferta.loja,
      preco: oferta.preco,
      endereco: oferta.endereco,
      maisBarata: produto.ofertas.length > 1 && oferta.loja === produto.melhorOferta?.loja,
    }));
  });
  protected readonly atualizadoEm = computed(() => {
    const instantes = this.produto()?.ofertas.map((oferta) => Date.parse(oferta.coletadoEm)).filter((valor) => !Number.isNaN(valor)) ?? [];
    if (instantes.length === 0) {
      return null;
    }
    const maisRecente = Math.max(...instantes);
    return `${diaMesEAno(maisRecente)} às ${hora(maisRecente)}`;
  });
  protected readonly topo = computed<TopoExibido | null>(() => {
    const produto = this.produto();
    if (produto === null) {
      return null;
    }
    const historico = this.historico();
    return {
      nome: produto.nome,
      tipo: produto.tipo,
      rotulo: produto.tipo === 'JOGO' ? 'Jogo' : (produto.categoria ?? 'Hardware'),
      capa: produto.capa,
      leitura: historico === null ? null : leituraDoPreco(produto.melhorOferta, historico),
    };
  });
  protected readonly observadoDesde = computed(() => {
    const desde = this.historico()?.observadoDesde;
    return desde == null ? null : diaMesEAno(desde);
  });

  constructor() {
    effect(() => {
      const estado = this.estadoDoProduto();
      if (estado.tipo === 'sucesso') {
        this.titulo.setTitle(`${estado.dados.nome} · ${MARCA}`);
      } else if (estado.tipo === 'vazio') {
        this.titulo.setTitle(`${TITULO_NAO_ENCONTRADO} · ${MARCA}`);
      }
    });
  }

  protected escolherPeriodo(periodo: PeriodoDoHistorico): void {
    this.periodoEscolhido.set(periodo);
  }

  protected alternarPorLoja(): void {
    this.porLoja.update((atual) => !atual);
  }

  protected lojaTravada(loja: string): boolean {
    const escolhidas = this.lojasEscolhidas();
    return escolhidas.includes(loja) ? escolhidas.length === 1 : escolhidas.length >= MAXIMO_DE_LOJAS_NO_GRAFICO;
  }

  protected alternarLoja(loja: string): void {
    this.lojasEscolhidas.update((atuais) => {
      if (atuais.includes(loja)) {
        return atuais.length > 1 ? atuais.filter((atual) => atual !== loja) : atuais;
      }
      return atuais.length < MAXIMO_DE_LOJAS_NO_GRAFICO ? [...atuais, loja] : atuais;
    });
  }

  protected tentarProdutoDeNovo(): void {
    this.recursoDoProduto.reload();
  }

  protected tentarHistoricoDeNovo(): void {
    this.recursoDoHistorico.reload();
  }

  protected aoMudarMonitoramento(monitoramento: MonitoramentoDoProduto | null): void {
    this.recursoDoMonitoramento.set({ tipo: 'sucesso', dados: monitoramento });
  }
}

async function estadoDoProduto(carga: () => Promise<ProdutoDetalhado>): Promise<EstadoDoBloco<ProdutoDetalhado>> {
  try {
    return { tipo: 'sucesso', dados: await carga() };
  } catch (falha) {
    if (casoDe(falha).tipo === 'nao-encontrado') {
      return { tipo: 'vazio' };
    }
    const mensagem = mensagemDoErro(falha);
    return mensagem === null ? { tipo: 'carregando' } : { tipo: 'erro', mensagem };
  }
}
