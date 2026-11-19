import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../data/auth.service';
import { CadastroPage } from './cadastro.page';

describe('CadastroPage', () => {
  let fixture: ComponentFixture<CadastroPage>;
  let auth: { provedores: string[]; cadastrar: ReturnType<typeof vi.fn>; iniciarLoginExterno: ReturnType<typeof vi.fn> };
  let router: Router;

  async function abrir(returnUrl?: string): Promise<HTMLElement> {
    auth = { provedores: ['discord'], cadastrar: vi.fn(), iniciarLoginExterno: vi.fn() };
    TestBed.configureTestingModule({
      imports: [CadastroPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CadastroPage);
    if (returnUrl) {
      fixture.componentRef.setInput('returnUrl', returnUrl);
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function preencher(tela: HTMLElement, id: string, valor: string): void {
    const campo = tela.querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    campo.dispatchEvent(new Event('blur'));
  }

  async function preencherEEnviar(tela: HTMLElement): Promise<void> {
    preencher(tela, 'cadastro-nome', 'Ana');
    preencher(tela, 'cadastro-email', 'ana@exemplo.com');
    preencher(tela, 'cadastro-senha', 'segredo123');
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  const erroDe = (tela: HTMLElement, id: string): string | undefined => {
    const descricao = tela.querySelector(`#${id}`)!.getAttribute('aria-describedby') ?? '';
    return descricao
      .split(' ')
      .map((parte) => tela.querySelector(`#${parte}`)?.textContent ?? '')
      .join(' ');
  };

  it('oferece continuar com o provedor e liga a dica da senha ao campo', async () => {
    const tela = await abrir();

    expect(tela.querySelector('umbra-botoes-login-externo button')?.textContent).toContain('Continuar com Discord');
    expect(erroDe(tela, 'cadastro-senha')).toContain('Mínimo de 8 caracteres.');
  });

  it('exige senha com o minimo do contrato', async () => {
    const tela = await abrir();

    preencher(tela, 'cadastro-senha', 'curta');
    await fixture.whenStable();

    expect(erroDe(tela, 'cadastro-senha')).toContain('Use pelo menos 8 caracteres.');
  });

  it('recusa senha acima do limite do bcrypt', async () => {
    const tela = await abrir();

    preencher(tela, 'cadastro-senha', 'é'.repeat(37));
    await fixture.whenStable();

    expect(erroDe(tela, 'cadastro-senha')).toContain('Senha longa demais');
  });

  it('nao envia formulario invalido e foca o nome', async () => {
    const tela = await abrir();

    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(auth.cadastrar).not.toHaveBeenCalled();
    expect(document.activeElement?.id).toBe('cadastro-nome');
  });

  it('cria a conta e segue para o destino ja logado', async () => {
    const tela = await abrir('/monitoramentos');
    auth.cadastrar.mockResolvedValue({ tipo: 'ok' });

    await preencherEEnviar(tela);

    expect(auth.cadastrar).toHaveBeenCalledWith({ nome: 'Ana', email: 'ana@exemplo.com', senha: 'segredo123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/monitoramentos');
  });

  it('leva ao login com o email quando a entrada automatica falha', async () => {
    const tela = await abrir('/monitoramentos');
    auth.cadastrar.mockResolvedValue({ tipo: 'criada-sem-entrar' });

    await preencherEEnviar(tela);

    expect(router.navigate).toHaveBeenCalledWith(['/entrar'], {
      queryParams: { email: 'ana@exemplo.com', conta: 'criada', returnUrl: '/monitoramentos' },
    });
  });

  it('mostra no campo o email ja usado e foca nele', async () => {
    const tela = await abrir();
    auth.cadastrar.mockResolvedValue({ tipo: 'email-em-uso' });

    await preencherEEnviar(tela);
    await fixture.whenStable();

    expect(erroDe(tela, 'cadastro-email')).toContain('Este email já tem conta.');
    expect(document.activeElement?.id).toBe('cadastro-email');
  });

  it('mostra no campo o erro devolvido pela API', async () => {
    const tela = await abrir();
    auth.cadastrar.mockResolvedValue({ tipo: 'invalido', erros: [{ campo: 'nome', mensagem: 'deve ter no máximo 60 caracteres' }] });

    await preencherEEnviar(tela);
    await fixture.whenStable();

    expect(erroDe(tela, 'cadastro-nome')).toContain('Este campo deve ter no máximo 60 caracteres.');
  });

  it('mostra falha geral sem perder o que foi digitado', async () => {
    const tela = await abrir();
    auth.cadastrar.mockResolvedValue({ tipo: 'falha' });

    await preencherEEnviar(tela);

    expect(tela.querySelector('[role="alert"]')?.textContent).toContain('Não foi possível criar a conta agora');
    expect(tela.querySelector<HTMLInputElement>('#cadastro-email')!.value).toBe('ana@exemplo.com');
  });

  it('ignora o segundo envio enquanto o primeiro esta em curso', async () => {
    const tela = await abrir();
    auth.cadastrar.mockReturnValue(new Promise(() => undefined));

    preencher(tela, 'cadastro-nome', 'Ana');
    preencher(tela, 'cadastro-email', 'ana@exemplo.com');
    preencher(tela, 'cadastro-senha', 'segredo123');
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(auth.cadastrar).toHaveBeenCalledTimes(1);
  });

  it('sai para o provedor guardando o destino', async () => {
    const tela = await abrir('/monitoramentos');

    tela.querySelector<HTMLButtonElement>('umbra-botoes-login-externo button')!.click();

    expect(auth.iniciarLoginExterno).toHaveBeenCalledWith('discord', '/monitoramentos');
  });

  it('voltar do provedor pelo navegador destrava os botoes', async () => {
    const tela = await abrir('/monitoramentos');
    tela.querySelector<HTMLButtonElement>('umbra-botoes-login-externo button')!.click();
    await fixture.whenStable();
    expect(tela.querySelector<HTMLButtonElement>('umbra-botoes-login-externo button')!.disabled).toBe(true);

    window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
    await fixture.whenStable();

    expect(tela.querySelector<HTMLButtonElement>('umbra-botoes-login-externo button')!.disabled).toBe(false);
  });
});
