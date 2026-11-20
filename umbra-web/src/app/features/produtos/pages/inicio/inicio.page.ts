import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subject, startWith, switchMap } from 'rxjs';
import { estadoDoFluxo } from '../../../../core/erro';
import { SessaoService } from '../../../../core/sessao';
import { MascoteComponent } from '../../../../shared/mascote/mascote.component';
import { EstadoDoBloco, dadosDe } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco';
import { EstadoDoBlocoComponent } from '../../../../shared/ui/estado-do-bloco/estado-do-bloco.component';
import { CardProdutoComponent } from '../../components/card-produto/card-produto.component';
import { CatalogoService } from '../../data/catalogo.service';
import { ProdutoDoCatalogo } from '../../data/produto-do-catalogo';
import { normalizarTermo, situacaoDoTermo } from '../catalogo/filtro-da-url';

const CARDS_NA_PRIMEIRA_FILEIRA = 4;

@Component({
  selector: 'umbra-inicio-page',
  imports: [CardProdutoComponent, EstadoDoBlocoComponent, MascoteComponent, RouterLink],
  templateUrl: './inicio.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioPage {
  private readonly catalogo = inject(CatalogoService);
  private readonly router = inject(Router);
  private readonly recarga = new Subject<void>();

  protected readonly autenticado = inject(SessaoService).autenticado;
  protected readonly cardsNaPrimeiraFileira = CARDS_NA_PRIMEIRA_FILEIRA;
  protected readonly parametrosDoCatalogo = { tipo: 'JOGO', ordenacao: 'melhoresOfertas' };

  protected readonly destaques = toSignal(
    this.recarga.pipe(
      startWith(undefined),
      switchMap(() => estadoDoFluxo(this.catalogo.destaques(), (produtos) => produtos.length === 0)),
    ),
    { initialValue: { tipo: 'carregando' } as EstadoDoBloco<readonly ProdutoDoCatalogo[]> },
  );
  protected readonly produtos = computed(() => dadosDe(this.destaques()) ?? []);

  protected readonly textoDigitado = signal('');
  protected readonly termoCurto = signal(false);
  protected readonly situacao = computed(() => situacaoDoTermo(this.textoDigitado()));

  protected digitar(texto: string): void {
    this.textoDigitado.set(texto);
    this.termoCurto.set(false);
  }

  protected buscar(evento: Event): void {
    evento.preventDefault();
    if (this.situacao() !== 'valido') {
      this.termoCurto.set(true);
      return;
    }
    void this.router.navigate(['/catalogo'], { queryParams: { nome: normalizarTermo(this.textoDigitado()) } });
  }

  protected recarregar(): void {
    this.recarga.next();
  }
}
