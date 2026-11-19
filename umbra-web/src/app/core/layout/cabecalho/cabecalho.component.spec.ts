import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessaoService, UsuarioDaSessao } from '../../sessao';
import { CabecalhoComponent } from './cabecalho.component';

describe('CabecalhoComponent', () => {
  let fixture: ComponentFixture<CabecalhoComponent>;
  let tela: HTMLElement;
  const autenticado = signal(false);
  const usuario = signal<UsuarioDaSessao | null>(null);
  const sair = vi.fn(() => autenticado.set(false));
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    autenticado.set(false);
    usuario.set(null);
    sair.mockClear();
    TestBed.configureTestingModule({
      imports: [CabecalhoComponent],
      providers: [provideRouter([]), { provide: SessaoService, useValue: { autenticado, usuario, sair } }],
    });
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(CabecalhoComponent);
    tela = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  async function entrarComo(nome: string): Promise<HTMLButtonElement> {
    autenticado.set(true);
    usuario.set({ nome, nomeCompleto: `${nome} da Silva`, email: 'ana@exemplo.com' });
    await fixture.whenStable();
    return tela.querySelector<HTMLButtonElement>('button[aria-controls="menu-da-conta"]')!;
  }

  async function clicar(elemento: HTMLElement): Promise<void> {
    elemento.click();
    await fixture.whenStable();
  }

  it('mostra Entrar para quem nao tem sessao', () => {
    expect(tela.querySelector('a[href="/entrar"]')?.textContent?.trim()).toBe('Entrar');
    expect(tela.querySelector('#menu-da-conta')).toBeNull();
  });

  it('mostra Conta enquanto o usuario carrega', async () => {
    autenticado.set(true);
    await fixture.whenStable();

    expect(tela.querySelector('button[aria-controls="menu-da-conta"]')?.textContent).toContain('Conta');
  });

  it('abre o menu da conta com o email e o botao Sair', async () => {
    const botao = await entrarComo('Ana');
    expect(botao.textContent).toContain('Ana');
    expect(botao.getAttribute('aria-label')).toBe('Conta de Ana da Silva');
    expect(botao.getAttribute('aria-expanded')).toBe('false');

    await clicar(botao);

    expect(botao.getAttribute('aria-expanded')).toBe('true');
    expect(tela.querySelector('#menu-da-conta')?.textContent).toContain('ana@exemplo.com');
  });

  it('sai, fecha o menu e volta ao catalogo', async () => {
    await clicar(await entrarComo('Ana'));

    const botaoSair = [...tela.querySelectorAll<HTMLButtonElement>('#menu-da-conta button')].find(
      (botao) => botao.textContent?.trim() === 'Sair',
    )!;
    await clicar(botaoSair);

    expect(sair).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith('/catalogo');
    expect(tela.querySelector('#menu-da-conta')).toBeNull();
  });

  it('fecha com Escape e devolve o foco ao botao da conta', async () => {
    const botao = await entrarComo('Ana');
    await clicar(botao);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await fixture.whenStable();

    expect(tela.querySelector('#menu-da-conta')).toBeNull();
    expect(document.activeElement).toBe(botao);
  });

  it('fecha com clique fora e ignora clique dentro', async () => {
    const botao = await entrarComo('Ana');
    await clicar(botao);

    await clicar(tela.querySelector<HTMLElement>('#menu-da-conta p')!);
    expect(tela.querySelector('#menu-da-conta')).not.toBeNull();

    await clicar(document.body);
    expect(tela.querySelector('#menu-da-conta')).toBeNull();
  });

  it('Escape com o menu fechado nao rouba o foco', async () => {
    await entrarComo('Ana');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(document.activeElement).not.toBe(tela.querySelector('button[aria-controls="menu-da-conta"]'));
  });
});
