import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../data/auth.service';
import { EntrarPage } from './entrar.page';

describe('EntrarPage', () => {
  let fixture: ComponentFixture<EntrarPage>;
  let auth: { provedores: string[]; entrar: ReturnType<typeof vi.fn>; iniciarLoginExterno: ReturnType<typeof vi.fn> };
  let navegar: ReturnType<typeof vi.spyOn>;

  async function abrir(entradas: Record<string, string> = {}): Promise<HTMLElement> {
    auth = { provedores: ['discord', 'google'], entrar: vi.fn(), iniciarLoginExterno: vi.fn() };
    TestBed.configureTestingModule({
      imports: [EntrarPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(EntrarPage);
    Object.entries(entradas).forEach(([nome, valor]) => fixture.componentRef.setInput(nome, valor));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function preencher(tela: HTMLElement, id: string, valor: string): void {
    const campo = tela.querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    campo.dispatchEvent(new Event('blur'));
  }

  async function enviar(tela: HTMLElement): Promise<void> {
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('mostra os dois provedores antes do email', async () => {
    const tela = await abrir();

    const botoes = [...tela.querySelectorAll('umbra-botoes-login-externo button')].map((b) => b.textContent?.trim());
    expect(botoes).toEqual(['Entrar com Discord', 'Entrar com Google']);
  });

  it('avisa a sessao expirada', async () => {
    const tela = await abrir({ motivo: 'expirada' });

    expect(tela.querySelector('[role="status"]')?.textContent).toContain('Sua sessão expirou');
  });

  it('avisa a conta criada e preenche o email', async () => {
    const tela = await abrir({ conta: 'criada', email: 'ana@exemplo.com' });

    expect(tela.textContent).toContain('Conta criada');
    expect(tela.querySelector<HTMLInputElement>('#entrar-email')!.value).toBe('ana@exemplo.com');
  });

  it('valida ao sair do campo e liga o erro ao campo', async () => {
    const tela = await abrir();

    preencher(tela, 'entrar-email', 'ana@');
    await fixture.whenStable();

    const campo = tela.querySelector('#entrar-email')!;
    expect(campo.getAttribute('aria-invalid')).toBe('true');
    expect(tela.querySelector(`#${campo.getAttribute('aria-describedby')}`)?.textContent).toContain('email válido');
  });

  it('nao envia formulario invalido e foca o primeiro campo com erro', async () => {
    const tela = await abrir();

    await enviar(tela);

    expect(auth.entrar).not.toHaveBeenCalled();
    expect(document.activeElement?.id).toBe('entrar-email');
  });

  it('entra e volta ao destino interno', async () => {
    const tela = await abrir({ returnUrl: '/monitoramentos' });
    auth.entrar.mockResolvedValue('ok');

    preencher(tela, 'entrar-email', 'ana@exemplo.com');
    preencher(tela, 'entrar-senha', 'segredo123');
    await enviar(tela);

    expect(auth.entrar).toHaveBeenCalledWith({ email: 'ana@exemplo.com', senha: 'segredo123' });
    expect(navegar).toHaveBeenCalledWith('/monitoramentos');
  });

  it('recusa destino externo e volta ao catalogo', async () => {
    const tela = await abrir({ returnUrl: 'https://malicioso.example' });
    auth.entrar.mockResolvedValue('ok');

    preencher(tela, 'entrar-email', 'ana@exemplo.com');
    preencher(tela, 'entrar-senha', 'segredo123');
    await enviar(tela);

    expect(navegar).toHaveBeenCalledWith('/catalogo');
  });

  it.each([
    ['credenciais', 'Email ou senha incorretos.'],
    ['falha', 'Não foi possível entrar agora.'],
  ])('mostra a recusa %s sem revelar se o email existe', async (resultado, mensagem) => {
    const tela = await abrir();
    auth.entrar.mockResolvedValue(resultado);

    preencher(tela, 'entrar-email', 'ana@exemplo.com');
    preencher(tela, 'entrar-senha', 'errada');
    await enviar(tela);

    expect(tela.querySelector('[role="alert"]')?.textContent).toContain(mensagem);
    expect(tela.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('ignora o segundo envio enquanto o primeiro esta em curso', async () => {
    const tela = await abrir();
    auth.entrar.mockReturnValue(new Promise(() => undefined));

    preencher(tela, 'entrar-email', 'ana@exemplo.com');
    preencher(tela, 'entrar-senha', 'segredo123');
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(auth.entrar).toHaveBeenCalledTimes(1);
  });

  it('mostra e oculta a senha', async () => {
    const tela = await abrir();
    const alternar = tela.querySelector<HTMLButtonElement>('button[aria-controls="entrar-senha"]')!;

    alternar.click();
    await fixture.whenStable();

    expect(tela.querySelector<HTMLInputElement>('#entrar-senha')!.type).toBe('text');
    expect(alternar.getAttribute('aria-pressed')).toBe('true');
    expect(alternar.textContent?.trim()).toBe('Ocultar');
  });

  it('sai para o provedor guardando o destino e trava os botoes', async () => {
    const tela = await abrir({ returnUrl: '/monitoramentos' });

    tela.querySelector<HTMLButtonElement>('umbra-botoes-login-externo button')!.click();
    await fixture.whenStable();

    expect(auth.iniciarLoginExterno).toHaveBeenCalledWith('discord', '/monitoramentos');
    const botoes = [...tela.querySelectorAll<HTMLButtonElement>('umbra-botoes-login-externo button')];
    expect(botoes.every((botao) => botao.disabled)).toBe(true);
    expect(botoes[0].getAttribute('aria-busy')).toBe('true');
  });
});
