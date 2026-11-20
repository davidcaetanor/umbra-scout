import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Observable, Subject } from 'rxjs';
import { ErroDaApi } from '../../../../core/erro';
import { SessaoService } from '../../../../core/sessao';
import { CatalogoService } from '../../data/catalogo.service';
import { ProdutoDoCatalogo } from '../../data/produto-do-catalogo';
import { InicioPage } from './inicio.page';

function produto(id: number, nome: string): ProdutoDoCatalogo {
  return {
    id,
    nome,
    tipo: 'JOGO',
    capa: null,
    categoria: null,
    preco: { atual: 'R$ 23,99', original: 'R$ 47,99', descontoPct: 50, loja: 'Steam' },
  };
}

class CatalogoFalso {
  pedidos = 0;
  resposta = new Subject<readonly ProdutoDoCatalogo[]>();

  destaques(): Observable<readonly ProdutoDoCatalogo[]> {
    this.pedidos++;
    this.resposta = new Subject<readonly ProdutoDoCatalogo[]>();
    return this.resposta;
  }
}

describe('InicioPage', () => {
  let catalogo: CatalogoFalso;
  let harness: RouterTestingHarness;
  let tela: HTMLElement;
  const autenticado = signal(false);

  async function abrir(): Promise<void> {
    catalogo = new CatalogoFalso();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '', component: InicioPage }]),
        { provide: CatalogoService, useValue: catalogo },
        { provide: SessaoService, useValue: { autenticado } },
      ],
    });
    harness = await RouterTestingHarness.create('/');
    tela = harness.routeNativeElement!;
  }

  async function responder(produtos: readonly ProdutoDoCatalogo[]): Promise<void> {
    catalogo.resposta.next(produtos);
    catalogo.resposta.complete();
    await harness.fixture.whenStable();
  }

  async function buscar(texto: string): Promise<void> {
    const campo = tela.querySelector<HTMLInputElement>('#busca-inicio')!;
    campo.value = texto;
    campo.dispatchEvent(new Event('input'));
    campo.form!.dispatchEvent(new Event('submit', { cancelable: true }));
    await harness.fixture.whenStable();
  }

  beforeEach(() => autenticado.set(false));

  it('mostra o esqueleto e depois os Destaques Umbra com a loja de cada um', async () => {
    await abrir();
    expect(tela.textContent).toContain('Carregando destaques');

    await responder([produto(1, 'Hades II'), produto(2, 'Celeste')]);

    expect(tela.querySelector('#titulo-destaques')?.textContent?.trim()).toBe('Destaques Umbra');
    expect([...tela.querySelectorAll('umbra-card-produto h3')].map((titulo) => titulo.textContent?.trim())).toEqual([
      'Hades II',
      'Celeste',
    ]);
    expect(tela.querySelector('umbra-card-produto')?.textContent).toContain('Steam');
  });

  it('ver todos leva ao catalogo de jogos pelas melhores ofertas', async () => {
    await abrir();

    const link = [...tela.querySelectorAll('a')].find((a) => a.textContent?.includes('Ver todos'));
    expect(link?.getAttribute('href')).toBe('/catalogo?tipo=JOGO&ordenacao=melhoresOfertas');
  });

  it('sem destaques convida a ver o catalogo', async () => {
    await abrir();
    await responder([]);

    expect(tela.textContent).toContain('Nenhum destaque agora');
  });

  it('erro mostra a tentativa de novo, que busca outra vez', async () => {
    await abrir();
    catalogo.resposta.error(new ErroDaApi({ tipo: 'tempo-esgotado' }));
    await harness.fixture.whenStable();

    [...tela.querySelectorAll('button')].find((b) => b.textContent?.includes('Tentar de novo'))!.click();
    await harness.fixture.whenStable();

    expect(catalogo.pedidos).toBe(2);
  });

  it('deslogado ve o convite e o Umbra no topo; logado ve o Umbra ao lado do titulo', async () => {
    await abrir();
    expect(tela.querySelector('a[href="/cadastro"]')).not.toBeNull();

    autenticado.set(true);
    await harness.fixture.whenStable();

    expect(tela.querySelector('a[href="/cadastro"]')).toBeNull();
    expect(tela.querySelector('#titulo-destaques')?.parentElement?.querySelector('umbra-mascote')).not.toBeNull();
  });

  it('busca leva o termo ao catalogo e termo curto so avisa', async () => {
    await abrir();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await buscar('ab');
    expect(tela.querySelector('#busca-inicio-dica')?.textContent).toContain('ao menos 3 letras');
    expect(navegar).not.toHaveBeenCalled();

    await buscar('  hollow ');
    expect(navegar).toHaveBeenCalledWith(['/catalogo'], { queryParams: { nome: 'hollow' } });
  });
});
