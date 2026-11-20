import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessaoService } from '../../sessao';
import { CabecalhoComponent } from './cabecalho.component';

describe('CabecalhoComponent, busca curta', () => {
  let fixture: ComponentFixture<CabecalhoComponent>;
  let tela: HTMLElement;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [CabecalhoComponent],
      providers: [
        provideRouter([
          { path: '', children: [] },
          { path: 'catalogo', children: [] },
          { path: 'conta', children: [] },
          { path: 'entrar', children: [] },
        ]),
        { provide: SessaoService, useValue: { autenticado: signal(false), usuario: signal(null), sair: vi.fn() } },
      ],
    });
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(CabecalhoComponent);
    tela = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  async function irPara(url: string): Promise<void> {
    await router.navigateByUrl(url);
    await fixture.whenStable();
  }

  function campo(): HTMLInputElement | null {
    return tela.querySelector<HTMLInputElement>('#busca-cabecalho');
  }

  it('nao aparece na inicial nem no catalogo, que ja tem busca propria, nem na entrada', async () => {
    await irPara('/');
    expect(campo()).toBeNull();

    await irPara('/entrar');
    expect(campo()).toBeNull();

    await irPara('/catalogo?nome=hollow');
    expect(campo()).toBeNull();
  });

  it('aparece nas outras telas e leva o termo ao catalogo, limpando o campo', async () => {
    await irPara('/conta');
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    campo()!.value = '  hollow knight ';

    campo()!.form!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(navegar).toHaveBeenCalledWith(['/catalogo'], { queryParams: { nome: 'hollow knight' } });
    expect(campo()!.value).toBe('');
  });

  it('termo curto nao navega e pede ao navegador para avisar', async () => {
    await irPara('/conta');
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const avisar = vi.spyOn(campo()!, 'reportValidity');
    campo()!.value = 'ab';

    campo()!.form!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(navegar).not.toHaveBeenCalled();
    expect(avisar).toHaveBeenCalled();
  });
});
