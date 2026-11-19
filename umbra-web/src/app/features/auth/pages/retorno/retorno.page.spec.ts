import { Location } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { frasesEmLinhas } from '../../../../shared/formatacao/frases-em-linhas.pipe';
import { AuthService } from '../../data/auth.service';
import { RetornoPage, mensagemDoMotivo } from './retorno.page';

describe('RetornoPage', () => {
  let fixture: ComponentFixture<RetornoPage>;
  let auth: {
    provedores: string[];
    trocarCodigo: ReturnType<typeof vi.fn>;
    retomarDestino: ReturnType<typeof vi.fn>;
    iniciarLoginExterno: ReturnType<typeof vi.fn>;
  };
  let substituir: ReturnType<typeof vi.spyOn>;
  let navegar: ReturnType<typeof vi.spyOn>;

  async function abrir(entradas: Record<string, string>): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [RetornoPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    substituir = vi.spyOn(TestBed.inject(Location), 'replaceState');
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(RetornoPage);
    Object.entries(entradas).forEach(([nome, valor]) => fixture.componentRef.setInput(nome, valor));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    auth = {
      provedores: ['discord'],
      trocarCodigo: vi.fn(),
      retomarDestino: vi.fn().mockReturnValue('/monitoramentos'),
      iniciarLoginExterno: vi.fn(),
    };
  });

  it('tira o codigo da barra antes de troca-lo', async () => {
    auth.trocarCodigo.mockImplementation(() => {
      expect(substituir).toHaveBeenCalledWith('/login/callback');
      return Promise.resolve('ok');
    });

    await abrir({ codigo: 'abc' });

    expect(auth.trocarCodigo).toHaveBeenCalledWith('abc');
  });

  it('mostra que esta entrando enquanto troca o codigo', async () => {
    auth.trocarCodigo.mockReturnValue(new Promise(() => undefined));

    const tela = await abrir({ codigo: 'abc' });

    expect(tela.querySelector('[role="status"] h1')?.textContent).toContain('Entrando');
  });

  it('volta ao destino guardado sem deixar o retorno no historico', async () => {
    auth.trocarCodigo.mockResolvedValue('ok');

    await abrir({ codigo: 'abc' });

    expect(navegar).toHaveBeenCalledWith('/monitoramentos', { replaceUrl: true });
  });

  it('recusa destino guardado que nao seja interno', async () => {
    auth.trocarCodigo.mockResolvedValue('ok');
    auth.retomarDestino.mockReturnValue('//malicioso.example');

    await abrir({ codigo: 'abc' });

    expect(navegar).toHaveBeenCalledWith('/catalogo', { replaceUrl: true });
  });

  it.each(['codigo-invalido', 'falha'])('mostra a falha da troca %s', async (resultado) => {
    auth.trocarCodigo.mockResolvedValue(resultado);

    const tela = await abrir({ codigo: 'abc' });

    expect(tela.querySelector('h1')?.textContent).toContain('Não deu para entrar');
    expect(tela.querySelector('[role="alert"]')?.textContent).toContain(frasesEmLinhas(mensagemDoMotivo(resultado)));
  });

  it.each(['email_nao_verificado', 'conta_inativa', 'conta_vinculada_a_outra_identidade', 'falha_no_provedor'])(
    'mostra o motivo %s vindo do backend sem trocar codigo',
    async (motivo) => {
      const tela = await abrir({ erro: motivo });

      expect(auth.trocarCodigo).not.toHaveBeenCalled();
      expect(substituir).toHaveBeenCalledWith('/login/callback');
      expect(tela.querySelector('[role="alert"]')?.textContent).toContain(frasesEmLinhas(mensagemDoMotivo(motivo)));
    },
  );

  it('trata motivo desconhecido e retorno vazio como falha do provedor', async () => {
    expect(mensagemDoMotivo('inventado')).toBe(mensagemDoMotivo('falha_no_provedor'));

    const tela = await abrir({});

    expect(tela.querySelector('[role="alert"]')?.textContent).toContain(frasesEmLinhas(mensagemDoMotivo('falha_no_provedor')));
  });

  it('permite tentar de novo pelo provedor com o destino original', async () => {
    const tela = await abrir({ erro: 'falha_no_provedor' });

    tela.querySelector<HTMLButtonElement>('umbra-botoes-login-externo button')!.click();

    expect(auth.iniciarLoginExterno).toHaveBeenCalledWith('discord', '/monitoramentos');
  });
});
