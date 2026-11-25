import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ErroDaApi } from '../../../../../core/erro';
import { TokenDaSessao } from '../../../../../core/sessao';
import { AvisoService } from '../../../../../shared/ui/aviso/aviso.service';
import { MonitoramentoDoProdutoService } from '../../../data/monitoramento-do-produto.service';
import { ProdutoExterno } from '../../../data/produto-externo';
import { CHAVE_DO_RASCUNHO, rascunhoDoJogoExterno } from '../../../data/rascunho-do-alvo';
import { ForaDoCatalogoComponent } from './fora-do-catalogo.component';

const HADES: ProdutoExterno = {
  gid: 'g-hades',
  nome: 'Hades II',
  capa: null,
  preco: { atual: 'R$ 69,90', original: null, descontoPct: null, loja: 'Steam' },
  precoCentavos: 6990,
  gratuito: false,
  enderecoDaLoja: 'https://loja.exemplo/hades',
};

const GRATIS: ProdutoExterno = { ...HADES, gid: 'g-gratis', nome: 'Jogo Grátis', gratuito: true, precoCentavos: 0 };

describe('ForaDoCatalogoComponent', () => {
  let fixture: ComponentFixture<ForaDoCatalogoComponent>;
  let tela: HTMLElement;
  let logado: boolean;
  let enviados: { gid: string; alvo: number }[];
  let falha: unknown;
  let avisos: string[];

  beforeAll(() => {
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.open = true;
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.open = false;
    };
  });

  beforeEach(() => {
    sessionStorage.clear();
    logado = true;
    enviados = [];
    falha = null;
    avisos = [];
  });

  async function abrir(produtos: ProdutoExterno[] = [HADES]): Promise<void> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: MonitoramentoDoProdutoService,
          useValue: {
            criarPorGid: (gid: string, alvo: number) => {
              enviados.push({ gid, alvo });
              return falha === null ? Promise.resolve({ produtoId: 77, precoAlvoCentavos: alvo }) : Promise.reject(falha);
            },
          },
        },
        { provide: TokenDaSessao, useValue: { vigente: () => (logado ? 'token' : null) } },
        { provide: AvisoService, useValue: { mostrar: (texto: string) => avisos.push(texto) } },
      ],
    });
    fixture = TestBed.createComponent(ForaDoCatalogoComponent);
    fixture.componentRef.setInput('produtos', produtos);
    tela = fixture.nativeElement as HTMLElement;
    await estabilizar();
  }

  async function estabilizar(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  const dialogo = () => tela.querySelector('dialog')!;
  const campo = () => dialogo().querySelector('input')!;
  const botao = (texto: string) => [...tela.querySelectorAll('button, a')].find((el) => el.textContent?.trim().startsWith(texto)) as HTMLElement;

  async function monitorarCom(texto: string): Promise<void> {
    botao('Monitorar').click();
    await estabilizar();
    campo().value = texto;
    campo().dispatchEvent(new Event('input'));
    dialogo().querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await estabilizar();
  }

  it('jogo gratuito nao oferece monitorar, so ver na loja', async () => {
    await abrir([GRATIS]);

    expect(tela.textContent).not.toContain('Monitorar');
    expect(tela.textContent).toContain('Ver na loja');
  });

  it('abre o dialogo com o melhor preco de referencia e o foco no campo', async () => {
    await abrir();

    botao('Monitorar').click();
    await estabilizar();

    expect(dialogo().open).toBe(true);
    expect(dialogo().textContent).toContain('Melhor preço R$ 69,90 na Steam');
    expect(document.activeElement).toBe(campo());
  });

  it('logado inclui pelo gid, vira monitorando e leva o foco ao link do produto', async () => {
    await abrir();

    await monitorarCom('60');

    expect(enviados).toEqual([{ gid: 'g-hades', alvo: 6000 }]);
    expect(dialogo().open).toBe(false);
    expect(tela.textContent).toContain('Monitorando, alvo');
    expect(tela.textContent).toContain('R$ 60,00');
    expect(tela.querySelector('a[href="/catalogo/77"]')).toBe(document.activeElement);
    expect(tela.querySelector('[role="status"]')?.textContent).toContain('Hades II monitorado');
    expect(avisos).toEqual(['Pronto. Agora eu vigio Hades II para você.']);
  });

  it('visitante nao grava nada: guarda o rascunho e vai ao login com retorno', async () => {
    logado = false;
    await abrir();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await monitorarCom('60');

    expect(enviados).toEqual([]);
    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { returnUrl: '/' } });
    expect(JSON.parse(sessionStorage.getItem(CHAVE_DO_RASCUNHO)!)).toEqual({ alvo: 'gid:g-hades', alvoCentavos: 6000 });
  });

  it('na volta do login reabre o dialogo preenchido, com foco em monitorar, sem gravar sozinho', async () => {
    sessionStorage.setItem(CHAVE_DO_RASCUNHO, JSON.stringify({ alvo: rascunhoDoJogoExterno('g-hades'), alvoCentavos: 6000 }));
    await abrir();

    expect(dialogo().open).toBe(true);
    expect(campo().value).toBe('60,00');
    expect(document.activeElement?.textContent?.trim()).toBe('Monitorar');
    expect(enviados).toEqual([]);
  });

  it('ja monitorado vira orientacao com link, sem erro', async () => {
    await abrir();
    falha = new ErroDaApi({ tipo: 'conflito', detalhe: 'Monitoramento já existe.' });

    await monitorarCom('60');

    expect(dialogo().open).toBe(false);
    expect(tela.textContent).toContain('Você já monitora este jogo.');
    expect(tela.querySelector('a[href="/monitoramentos"]')).toBe(document.activeElement);
  });

  it('fonte ocupada mostra a espera no dialogo e mantem o alvo digitado', async () => {
    await abrir();
    falha = new ErroDaApi({ tipo: 'indisponivel', tentarDeNovoEmSegundos: 60 });

    await monitorarCom('60');

    expect(dialogo().open).toBe(true);
    expect(dialogo().querySelector('[role="alert"]')?.textContent).toContain('Tente de novo em 60 segundos.');
    expect(campo().value).toBe('60');
  });

  it('Esc fecha sem gravar e devolve o foco ao botao monitorar', async () => {
    await abrir();
    botao('Monitorar').click();
    await estabilizar();

    dialogo().dispatchEvent(new Event('cancel', { cancelable: true }));
    await estabilizar();

    expect(dialogo().open).toBe(false);
    expect(enviados).toEqual([]);
    expect(document.activeElement).toBe(botao('Monitorar'));
  });
});
