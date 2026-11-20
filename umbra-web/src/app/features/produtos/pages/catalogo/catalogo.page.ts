import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, catchError, concat, debounceTime, distinctUntilChanged, filter, map, of, startWith, switchMap } from 'rxjs';
import { estadoDoFluxo, mensagemDoErro } from '../../../../core/erro';
import { EstadoMascote } from '../../../../shared/mascote/estado-mascote';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';
import { EstadoDoBloco, dadosDe } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco';
import { EstadoDoBlocoComponent } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco.component';
import { CardExternoComponent } from '../../components/card-externo/card-externo.component';
import { CardProdutoComponent } from '../../components/card-produto/card-produto.component';
import { PaginacaoComponent } from '../../components/paginacao/paginacao.component';
import { CatalogoService } from '../../data/catalogo.service';
import {
  CategoriaDeHardware,
  CodigoDaLoja,
  DESCONTOS_MINIMOS,
  FiltroDaBusca,
  ORDENACOES_SO_DE_JOGO,
  OrdenacaoDaBusca,
  TipoDeProduto,
} from '../../data/filtro-da-busca';
import { ResultadoDaBusca } from '../../data/resultado-da-busca';
import { ROTULO_DA_CATEGORIA, ROTULO_DA_LOJA, ROTULO_DA_ORDENACAO, ROTULO_DO_TIPO } from '../../data/rotulos';
import { EstadoDaBuscaExterna } from './estado-da-busca-externa';
import {
  FILTRO_INICIAL,
  filtroDaUrl,
  lojasPara,
  mesmoFiltro,
  normalizarFiltro,
  normalizarTermo,
  parametrosDoFiltro,
  reaisInteiros,
  situacaoDoTermo,
} from './filtro-da-url';

const ESPERA_DA_DIGITACAO_EM_MS = 300;
const SEM_BUSCA_EXTERNA: EstadoDaBuscaExterna = { tipo: 'inativa' };

@Component({
  selector: 'umbra-catalogo-page',
  imports: [CardExternoComponent, CardProdutoComponent, EstadoDoBlocoComponent, MascoteComponent, PaginacaoComponent],
  templateUrl: './catalogo.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogoPage {
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogo = inject(CatalogoService);
  private readonly digitacao = new Subject<string>();
  private readonly pedidoDeBuscaExterna = new Subject<void>();
  private readonly recarga = new Subject<void>();

  protected readonly tipos = Object.entries(ROTULO_DO_TIPO) as [TipoDeProduto, string][];
  protected readonly categorias = Object.entries(ROTULO_DA_CATEGORIA) as [CategoriaDeHardware, string][];
  protected readonly descontos = DESCONTOS_MINIMOS;
  protected readonly rotuloDaLoja = ROTULO_DA_LOJA;

  protected readonly filtro = toSignal(
    this.rota.queryParamMap.pipe(map(filtroDaUrl), distinctUntilChanged(mesmoFiltro)),
    { initialValue: FILTRO_INICIAL },
  );

  protected readonly ordenacoes = computed(() =>
    (Object.entries(ROTULO_DA_ORDENACAO) as [OrdenacaoDaBusca, string][]).filter(
      ([ordenacao]) => this.filtro().tipo === 'JOGO' || !ORDENACOES_SO_DE_JOGO.includes(ordenacao),
    ),
  );
  protected readonly lojas = computed(() => lojasPara(this.filtro().tipo));
  protected readonly filtrosAtivos = computed(() => {
    const filtro = this.filtro();
    return [filtro.precoMin, filtro.precoMax, filtro.descontoMin, filtro.loja].filter((valor) => valor !== null).length;
  });
  protected readonly faixaInvertida = signal(false);
  protected readonly filtrosVisiveis = signal(false);

  protected readonly textoDigitado = signal(this.filtro().termo ?? '');
  protected readonly situacaoDoTexto = computed(() => situacaoDoTermo(this.textoDigitado()));

  protected readonly resultado = toSignal(
    toObservable(this.filtro).pipe(
      switchMap((filtro) =>
        this.recarga.pipe(
          startWith(undefined),
          switchMap(() => estadoDoFluxo(this.catalogo.buscar(filtro), (dados) => dados.total === 0)),
        ),
      ),
    ),
    { initialValue: { tipo: 'carregando' } as EstadoDoBloco<ResultadoDaBusca> },
  );
  protected readonly dados = computed(() => dadosDe(this.resultado()));

  protected readonly buscaExterna = toSignal(
    toObservable(this.filtro).pipe(
      switchMap((filtro) =>
        this.pedidoDeBuscaExterna.pipe(
          switchMap(() => this.buscarForaDoCatalogo(filtro)),
          startWith(SEM_BUSCA_EXTERNA),
        ),
      ),
    ),
    { initialValue: SEM_BUSCA_EXTERNA },
  );

  protected readonly nadaEncontrado = computed(() => {
    if (this.resultado().tipo !== 'vazio') {
      return false;
    }
    const externa = this.buscaExterna();
    return (
      this.filtro().termo === null ||
      externa.tipo === 'falhou' ||
      (externa.tipo === 'concluida' && externa.produtos.length === 0)
    );
  });

  protected readonly podeBuscarFora = computed(
    () => this.filtro().termo !== null && this.resultado().tipo === 'sucesso' && this.buscaExterna().tipo === 'inativa',
  );

  protected readonly umbraDoCabecalho = computed((): EstadoMascote | null => {
    if (this.resultado().tipo === 'erro' || this.nadaEncontrado()) {
      return null;
    }
    return this.buscaExterna().tipo === 'buscando' ? 'buscando' : 'observando';
  });

  protected readonly contagem = computed(() => {
    const total = this.dados()?.total;
    if (total === undefined) {
      return '';
    }
    return total === 1 ? '1 produto encontrado' : `${total.toLocaleString('pt-BR')} produtos encontrados`;
  });

  constructor() {
    this.digitacao
      .pipe(
        map(normalizarTermo),
        debounceTime(ESPERA_DA_DIGITACAO_EM_MS),
        filter((termo) => ['vazio', 'valido'].includes(situacaoDoTermo(termo))),
        distinctUntilChanged(),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((termo) => this.aplicar({ termo: termo === '' ? null : termo, pagina: 0 }, true));

    effect(() => {
      const termo = this.filtro().termo ?? '';
      untracked(() => {
        if (normalizarTermo(this.textoDigitado()) !== termo && situacaoDoTermo(this.textoDigitado()) !== 'curto') {
          this.textoDigitado.set(termo);
        }
      });
    });

    effect(() => {
      const filtro = this.filtro();
      const resultado = this.resultado();
      untracked(() => this.reagirAoResultado(filtro, resultado));
    });
  }

  protected digitar(texto: string): void {
    this.textoDigitado.set(texto);
    this.digitacao.next(texto);
  }

  protected escolherTipo(tipo: TipoDeProduto | null): void {
    this.aplicar({ tipo, categoria: tipo === 'HARDWARE' ? this.filtro().categoria : null, pagina: 0 });
  }

  protected escolherCategoria(valor: string): void {
    this.aplicar({ categoria: (valor || null) as CategoriaDeHardware | null, pagina: 0 });
  }

  protected escolherOrdenacao(valor: string): void {
    this.aplicar({ ordenacao: valor as OrdenacaoDaBusca, pagina: 0 });
  }

  protected escolherPreco(campo: 'precoMin' | 'precoMax', valor: string): void {
    const reais = reaisInteiros(valor);
    const proximo = { ...this.filtro(), [campo]: reais };
    const invertida = proximo.precoMin !== null && proximo.precoMax !== null && proximo.precoMax < proximo.precoMin;
    this.faixaInvertida.set(invertida);
    if (!invertida) {
      this.aplicar({ [campo]: reais, pagina: 0 });
    }
  }

  protected escolherDesconto(valor: string): void {
    this.aplicar({ descontoMin: valor === '' ? null : Number(valor), pagina: 0 });
  }

  protected escolherLoja(valor: string): void {
    this.aplicar({ loja: (valor || null) as CodigoDaLoja | null, pagina: 0 });
  }

  protected irParaPagina(pagina: number): void {
    this.aplicar({ pagina });
  }

  protected buscarFora(): void {
    this.pedidoDeBuscaExterna.next();
  }

  protected limparFiltros(): void {
    this.textoDigitado.set('');
    this.faixaInvertida.set(false);
    this.aplicar(FILTRO_INICIAL);
  }

  protected recarregar(): void {
    this.recarga.next();
  }

  private reagirAoResultado(filtro: FiltroDaBusca, resultado: EstadoDoBloco<ResultadoDaBusca>): void {
    if (resultado.tipo === 'sucesso' && filtro.pagina >= resultado.dados.totalDePaginas) {
      this.aplicar({ pagina: resultado.dados.totalDePaginas - 1 }, true);
    }
    if (resultado.tipo === 'vazio' && filtro.termo !== null && filtro.pagina === 0) {
      this.pedidoDeBuscaExterna.next();
    }
  }

  private buscarForaDoCatalogo(filtro: FiltroDaBusca) {
    return concat(
      of<EstadoDaBuscaExterna>({ tipo: 'buscando' }),
      this.catalogo.buscarForaDoCatalogo(filtro).pipe(
        map((produtos): EstadoDaBuscaExterna => ({ tipo: 'concluida', produtos })),
        catchError((erro: unknown) =>
          of<EstadoDaBuscaExterna>({ tipo: 'falhou', mensagem: mensagemDoErro(erro) ?? '' }),
        ),
      ),
    );
  }

  private aplicar(mudanca: Partial<FiltroDaBusca>, substituir = false): void {
    const proximo = normalizarFiltro({ ...this.filtro(), ...mudanca });
    if (mesmoFiltro(proximo, this.filtro())) {
      return;
    }
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: parametrosDoFiltro(proximo),
      replaceUrl: substituir,
    });
  }
}
