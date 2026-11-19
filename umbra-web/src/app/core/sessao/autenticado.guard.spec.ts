import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { autenticadoGuard } from './autenticado.guard';
import { RELOGIO } from './relogio';
import { TokenDaSessao } from './token-da-sessao';
import { AGORA, tokenQueExpiraEm } from './token-de-teste';

describe('autenticadoGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RELOGIO, useValue: () => AGORA }] });
  });

  function ativar(url: string): boolean | UrlTree {
    return TestBed.runInInjectionContext(
      () => autenticadoGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as boolean | UrlTree,
    );
  }

  it('libera quem tem sessao vigente', () => {
    TestBed.inject(TokenDaSessao).definir(tokenQueExpiraEm(3_600_000));

    expect(ativar('/monitoramentos')).toBe(true);
  });

  it('manda ao login com o destino de volta', () => {
    const resultado = ativar('/monitoramentos?pagina=2');

    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe(
      '/entrar?returnUrl=%2Fmonitoramentos%3Fpagina%3D2',
    );
  });
});
