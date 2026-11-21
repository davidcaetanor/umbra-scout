import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ErroDaApi } from '../../../../../core/erro';
import { TokenDaSessao } from '../../../../../core/sessao';
import { AvisoService } from '../../../../../shared/ui/aviso/aviso.service';
import { MonitoramentoDoProdutoService } from '../../../data/monitoramento-do-produto.service';
import { MonitorarComponent, SituacaoDoMonitoramento } from './monitorar.component';
import { FAIXAS_DE_PIADAS } from './piadas-do-alvo';
import { CHAVE_DO_RASCUNHO, RascunhoDoAlvo } from './rascunho-do-alvo';

describe('MonitorarComponent', () => {
  let fixture: ComponentFixture<MonitorarComponent>;
  let criar: ReturnType<typeof vi.fn>;
  let logado: boolean;

  async function abrir(situacao: SituacaoDoMonitoramento, gratuito = false): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MonitoramentoDoProdutoService, useValue: { criar } },
        { provide: TokenDaSessao, useValue: { vigente: () => (logado ? 'token' : null) } },
      ],
    });
    fixture = TestBed.createComponent(MonitorarComponent);
    fixture.componentRef.setInput('produtoId', 42);
    fixture.componentRef.setInput('gratuito', gratuito);
    fixture.componentRef.setInput('situacao', situacao);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  async function enviar(tela: HTMLElement, valor: string): Promise<void> {
    const campo = tela.querySelector<HTMLInputElement>('#preco-alvo')!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    await new Promise((pronto) => setTimeout(pronto));
    await fixture.whenStable();
  }

  beforeEach(() => {
    sessionStorage.clear();
    criar = vi.fn();
    logado = true;
  });

  it('não oferece o formulário para produto grátis', async () => {
    const tela = await abrir({ tipo: 'livre' }, true);

    expect(tela.querySelector('form')).toBeNull();
    expect(tela.textContent).toContain('Este produto está grátis agora.');
  });

  it('mostra o alvo de quem já monitora e o caminho para a lista', async () => {
    const tela = await abrir({ tipo: 'monitorado', monitoramento: { id: 1, precoAlvoCentavos: 14990 } });

    expect(tela.textContent).toContain('R$ 149,90');
    expect(tela.querySelector('a')?.getAttribute('href')).toBe('/monitoramentos');
  });

  it('avisa enquanto confere os monitoramentos', async () => {
    const tela = await abrir({ tipo: 'conferindo' });

    expect(tela.textContent).toContain('Conferindo seus monitoramentos');
  });

  it('recusa campo vazio e valor fora do formato, ligando o erro ao campo', async () => {
    const tela = await abrir({ tipo: 'livre' });

    await enviar(tela, '');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('Informe o preço alvo.');

    await enviar(tela, 'abc');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('Use só números, como 149,90 ou 149.90.');

    await enviar(tela, '-10');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('Use só números, como 149,90 ou 149.90.');

    await enviar(tela, '0');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('O alvo precisa ser maior que zero.');

    await enviar(tela, '150.000,01');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('O alvo vai até R$ 150.000,00.');
    expect(tela.querySelector('#preco-alvo')?.getAttribute('aria-invalid')).toBe('true');
    expect(criar).not.toHaveBeenCalled();
  });

  it('deslogado guarda o alvo e vai ao login com a volta para o produto', async () => {
    logado = false;
    const tela = await abrir({ tipo: 'deslogado' });
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await enviar(tela, '149,90');

    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { returnUrl: '/' } });
    expect(TestBed.inject(RascunhoDoAlvo).retomar(42)).toBe(14990);
  });

  it('aceita o máximo com separador de milhar e ponto decimal', async () => {
    criar.mockResolvedValue({ id: 9, precoAlvoCentavos: 15_000_000 });
    const tela = await abrir({ tipo: 'livre' });

    await enviar(tela, '150.000,00');
    expect(criar).toHaveBeenLastCalledWith(42, 15_000_000);

    await enviar(tela, '149.90');
    expect(criar).toHaveBeenLastCalledWith(42, 14990);
  });

  it('cria o monitoramento, avisa com o retrato feliz e entrega o resultado', async () => {
    criar.mockResolvedValue({ id: 9, precoAlvoCentavos: 14990 });
    const tela = await abrir({ tipo: 'livre' });
    const criados: unknown[] = [];
    fixture.componentInstance.criado.subscribe((monitoramento) => criados.push(monitoramento));

    await enviar(tela, '149,90');

    expect(criar).toHaveBeenCalledWith(42, 14990);
    expect(TestBed.inject(AvisoService).atual()).toEqual({
      texto: 'Monitoramento criado. O Umbra avisa quando o preço chegar lá.',
      detalhe: null,
      figura: { tipo: 'retrato', retrato: 'feliz' },
    });
    expect(criados).toEqual([{ id: 9, precoAlvoCentavos: 14990 }]);
  });

  it('alvo acima de R$ 5 mil recebe a piada da faixa no lugar do aviso comum', async () => {
    localStorage.clear();
    criar.mockResolvedValue({ id: 9, precoAlvoCentavos: 600_000 });
    const tela = await abrir({ tipo: 'livre' });
    const [delirio, naoEPeca, caroMasExiste] = FAIXAS_DE_PIADAS;

    await enviar(tela, '5.000,00');
    const comum = TestBed.inject(AvisoService).atual();
    await enviar(tela, '5.000,01');
    const leve = TestBed.inject(AvisoService).atual();
    await enviar(tela, '50.000');
    const ironia = TestBed.inject(AvisoService).atual();
    await enviar(tela, '120.000');
    const absurdo = TestBed.inject(AvisoService).atual();
    await enviar(tela, '6000');
    const leveDeNovo = TestBed.inject(AvisoService).atual();

    expect(comum?.texto).toBe('Monitoramento criado. O Umbra avisa quando o preço chegar lá.');
    expect(leve).toEqual({ texto: caroMasExiste.piadas[0].texto, detalhe: 'Alvo salvo.', figura: caroMasExiste.piadas[0].figura });
    expect(ironia?.texto).toBe(naoEPeca.piadas[0].texto);
    expect(absurdo?.texto).toBe(delirio.piadas[0].texto);
    expect(leveDeNovo?.texto).toBe(caroMasExiste.piadas[1].texto);
  });

  it('mostra o digitando enquanto grava', async () => {
    criar.mockReturnValue(new Promise(() => undefined));
    const tela = await abrir({ tipo: 'livre' });

    await enviar(tela, '10');

    expect(tela.querySelector('button[type="submit"]')?.getAttribute('aria-busy')).toBe('true');
    expect(tela.querySelector('umbra-mascote')).not.toBeNull();
  });

  it('conflito leva a Meus monitoramentos', async () => {
    criar.mockRejectedValue(new ErroDaApi({ tipo: 'conflito', detalhe: null }));
    const tela = await abrir({ tipo: 'livre' });
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await enviar(tela, '10');

    expect(navegar).toHaveBeenCalledWith('/monitoramentos');
    expect(TestBed.inject(AvisoService).atual()?.texto).toBe('Você já monitora este produto.');
  });

  it('mostra a mensagem de outras falhas e de valor recusado pela API', async () => {
    criar.mockRejectedValueOnce(new ErroDaApi({ tipo: 'regra', detalhe: 'Limite de monitoramentos atingido.' }));
    const tela = await abrir({ tipo: 'livre' });

    await enviar(tela, '10');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('Limite de monitoramentos atingido.');

    criar.mockRejectedValueOnce(new ErroDaApi({ tipo: 'invalido', campos: [] }));
    await enviar(tela, '10');
    expect(tela.querySelector('[role="alert"]')?.textContent).toBe('Use só números, como 149,90 ou 149.90.');
  });

  it('logado retoma o rascunho deste produto e põe o foco no botão', async () => {
    sessionStorage.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ produtoId: 42, alvoCentavos: 7240 }));
    const tela = await abrir({ tipo: 'livre' });
    await fixture.whenStable();

    expect(tela.querySelector<HTMLInputElement>('#preco-alvo')?.value).toBe('72,40');
    expect(document.activeElement).toBe(tela.querySelector('button[type="submit"]'));
  });
});
