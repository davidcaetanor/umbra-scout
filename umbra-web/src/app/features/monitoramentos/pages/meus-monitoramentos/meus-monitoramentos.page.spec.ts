import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ErroDaApi } from '../../../../core/erro';
import { RELOGIO } from '../../../../core/sessao/relogio';
import { AvisoService } from '../../../../shared/ui/aviso/aviso.service';
import { MeusMonitoramentosService } from '../../data/meus-monitoramentos.service';
import { MonitoramentoListado } from '../../data/monitoramento-listado';
import { CHAVE_DA_RONDA, MeusMonitoramentosPage } from './meus-monitoramentos.page';

const AGORA = Date.parse('2026-11-24T10:00:00-03:00');

function item(id: number, nome: string, parcial: Partial<MonitoramentoListado> = {}): MonitoramentoListado {
  return {
    id,
    produtoId: id * 10,
    nome,
    capa: null,
    tipo: 'JOGO',
    precoAlvoCentavos: 15000,
    ativo: true,
    melhorPreco: {
      valorCentavos: 17249,
      loja: 'GreenManGaming',
      gratuito: false,
      situacao: 'vigente',
      expiraEm: null,
      verificadoEm: AGORA,
    },
    distanciaDoAlvoCentavos: 2249,
    alvoAtingido: false,
    foraDeVenda: false,
    ...parcial,
  };
}

const ATINGIDO = item(1, 'Hollow Knight: Silksong', {
  precoAlvoCentavos: 8000,
  melhorPreco: { valorCentavos: 6990, loja: 'Steam', gratuito: false, situacao: 'vigente', expiraEm: null, verificadoEm: AGORA },
  distanciaDoAlvoCentavos: -1010,
  alvoAtingido: true,
});

class ServicoFalso {
  itens: MonitoramentoListado[] = [];
  falhaAoRemover: unknown = null;
  removidos: number[] = [];

  listarTodos(): Promise<MonitoramentoListado[]> {
    return Promise.resolve([...this.itens]);
  }

  definirAtivo(id: number, ativo: boolean): Promise<MonitoramentoListado> {
    const atual = this.itens.find((existente) => existente.id === id)!;
    return Promise.resolve({ ...atual, ativo, alvoAtingido: ativo && atual.alvoAtingido });
  }

  remover(id: number): Promise<void> {
    if (this.falhaAoRemover !== null) {
      return Promise.reject(this.falhaAoRemover);
    }
    this.removidos.push(id);
    return Promise.resolve();
  }
}

describe('MeusMonitoramentosPage', () => {
  let servico: ServicoFalso;
  let harness: RouterTestingHarness;
  let tela: HTMLElement;
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
    localStorage.clear();
    servico = new ServicoFalso();
    avisos = [];
  });

  async function abrir(itens: MonitoramentoListado[]): Promise<void> {
    servico.itens = itens;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '', component: MeusMonitoramentosPage }]),
        { provide: MeusMonitoramentosService, useValue: servico },
        { provide: RELOGIO, useValue: () => AGORA },
        { provide: AvisoService, useValue: { mostrar: (texto: string) => avisos.push(texto) } },
      ],
    });
    harness = await RouterTestingHarness.create('/');
    tela = harness.routeNativeElement!;
    await estabilizar();
  }

  async function estabilizar(): Promise<void> {
    await harness.fixture.whenStable();
    harness.fixture.detectChanges();
    await harness.fixture.whenStable();
  }

  function botao(rotulo: string): HTMLButtonElement {
    return tela.querySelector<HTMLButtonElement>(`button[aria-label="${rotulo}"]`)!;
  }

  function dialogo(): HTMLDialogElement {
    return tela.querySelector('dialog')!;
  }

  function botaoDoDialogo(texto: string): HTMLButtonElement {
    return [...dialogo().querySelectorAll('button')].find((atual) => atual.textContent?.trim() === texto)!;
  }

  it('sem monitoramento ensina pelo balao do Umbra e leva ao catalogo', async () => {
    await abrir([]);

    expect(tela.querySelector('umbra-balao-fala')?.textContent).toContain('Escolha um produto no catálogo');
    expect(tela.querySelector('a[href="/catalogo"]')?.textContent?.trim()).toBe('Ir ao catálogo');
    expect(tela.querySelector('ul')).toBeNull();
  });

  it('lista com contagem e carimbo so no ativo que atingiu o alvo', async () => {
    await abrir([ATINGIDO, item(2, 'Black Myth: Wukong'), item(3, 'Elden Ring', { ativo: false })]);

    expect(tela.textContent).toContain('2 ativos · 1 pausado');
    const itens = [...tela.querySelectorAll('umbra-item-monitoramento')];
    expect(itens.map((atual) => atual.querySelector('h2')?.textContent?.trim())).toEqual([
      'Hollow Knight: Silksong',
      'Black Myth: Wukong',
      'Elden Ring',
    ]);
    expect(itens[0].textContent).toContain('Alvo atingido');
    expect(itens[2].textContent).toContain('Pausado');
    expect(itens[2].textContent).not.toContain('Alvo atingido');
  });

  it('novidade fala uma vez por sessao com produto, preco e loja', async () => {
    await abrir([ATINGIDO, item(2, 'Black Myth: Wukong')]);

    const fala = tela.querySelector('umbra-balao-fala')?.textContent ?? '';
    expect(fala).toContain('Achei!');
    expect(fala).toContain('Hollow Knight: Silksong');
    expect(fala).toContain('R$ 69,90');

    TestBed.resetTestingModule();
    await abrir([ATINGIDO, item(2, 'Black Myth: Wukong')]);

    expect(tela.textContent).not.toContain('Achei');
  });

  it('pausar o item da novidade cala o Umbra sobre ele', async () => {
    await abrir([ATINGIDO, item(2, 'Black Myth: Wukong')]);
    expect(tela.textContent).toContain('Achei!');

    botao('Pausar Hollow Knight: Silksong').click();
    await estabilizar();

    expect(tela.textContent).not.toContain('Achei!');
  });

  it('varios alvos atingidos viram uma fala so com a contagem', async () => {
    await abrir([ATINGIDO, item(2, 'Celeste', { alvoAtingido: true, distanciaDoAlvoCentavos: -100 })]);

    expect(tela.querySelector('umbra-balao-fala')?.textContent).toContain('Achei 2!');
    expect(tela.querySelector('umbra-balao-fala')?.textContent).toContain('mais 1 abaixo do alvo');
  });

  it('sem novidade a frase da ronda aparece so na primeira visita do dia', async () => {
    await abrir([item(2, 'Black Myth: Wukong')]);
    expect(tela.querySelector('umbra-balao-fala')).not.toBeNull();
    expect(localStorage.getItem(CHAVE_DA_RONDA)).toBe('2026-11-24');

    TestBed.resetTestingModule();
    await abrir([item(2, 'Black Myth: Wukong')]);

    expect(tela.querySelector('umbra-balao-fala')).toBeNull();
  });

  it('todos pausados mostra o Umbra cochilando e a fala propria', async () => {
    await abrir([item(2, 'Black Myth: Wukong', { ativo: false })]);

    expect(tela.querySelector('umbra-mascote')?.innerHTML).toContain('pausado-south');
    expect(tela.textContent).toContain('Tudo pausado');
  });

  it('pausar troca o botao para reativar, mantem o foco e anuncia', async () => {
    await abrir([item(2, 'Black Myth: Wukong')]);
    const pausar = botao('Pausar Black Myth: Wukong');
    pausar.focus();

    pausar.click();
    await estabilizar();

    const reativar = botao('Reativar Black Myth: Wukong');
    expect(reativar).toBe(pausar);
    expect(document.activeElement).toBe(reativar);
    expect(tela.querySelector('[role="status"]')?.textContent).toContain('Black Myth: Wukong pausado');
    expect(tela.textContent).toContain('1 pausado');
  });

  it('alvos atingidos ficam juntos no topo da lista', async () => {
    const outro = item(5, 'Celeste', { alvoAtingido: true, distanciaDoAlvoCentavos: -100 });
    await abrir([item(2, 'Black Myth: Wukong'), ATINGIDO, item(3, 'Elden Ring'), outro]);

    const nomes = [...tela.querySelectorAll('umbra-item-monitoramento h2')].map((titulo) => titulo.textContent?.trim());
    expect(nomes).toEqual(['Hollow Knight: Silksong', 'Celeste', 'Black Myth: Wukong', 'Elden Ring']);
  });

  it('remover pede confirmacao com foco em cancelar e cancelar devolve o foco', async () => {
    await abrir([item(2, 'Black Myth: Wukong')]);

    botao('Remover Black Myth: Wukong').click();
    await estabilizar();

    expect(dialogo().open).toBe(true);
    expect(dialogo().textContent).toContain('Remover Black Myth: Wukong?');
    expect(document.activeElement).toBe(botaoDoDialogo('Cancelar'));

    botaoDoDialogo('Cancelar').click();
    await estabilizar();

    expect(dialogo().open).toBe(false);
    expect(servico.removidos).toEqual([]);
    expect(document.activeElement).toBe(botao('Remover Black Myth: Wukong'));
  });

  it('Esc no dialogo cancela sem remover', async () => {
    await abrir([item(2, 'Black Myth: Wukong')]);
    botao('Remover Black Myth: Wukong').click();
    await estabilizar();

    dialogo().dispatchEvent(new Event('cancel', { cancelable: true }));
    await estabilizar();

    expect(dialogo().open).toBe(false);
    expect(servico.removidos).toEqual([]);
  });

  it('depois de remover o foco vai para o titulo do item seguinte', async () => {
    await abrir([item(2, 'Black Myth: Wukong'), item(3, 'Elden Ring')]);
    botao('Remover Black Myth: Wukong').click();
    await estabilizar();

    botaoDoDialogo('Remover').click();
    await estabilizar();

    expect(servico.removidos).toEqual([2]);
    expect(tela.querySelectorAll('umbra-item-monitoramento')).toHaveLength(1);
    expect(document.activeElement?.id).toBe('monitoramento-3');
    expect(tela.querySelector('[role="status"]')?.textContent).toContain('Black Myth: Wukong removido');
  });

  it('remover o ultimo item leva ao estado vazio com foco no titulo', async () => {
    await abrir([item(2, 'Black Myth: Wukong')]);
    botao('Remover Black Myth: Wukong').click();
    await estabilizar();

    botaoDoDialogo('Remover').click();
    await estabilizar();

    expect(tela.textContent).toContain('Ir ao catálogo');
    expect(document.activeElement?.id).toBe('titulo-da-pagina');
  });

  it('falha ao remover avisa, mantem o item e devolve o foco ao botao', async () => {
    await abrir([item(2, 'Black Myth: Wukong')]);
    servico.falhaAoRemover = new ErroDaApi({ tipo: 'inesperado' });
    botao('Remover Black Myth: Wukong').click();
    await estabilizar();

    botaoDoDialogo('Remover').click();
    await estabilizar();

    expect(avisos).toHaveLength(1);
    expect(tela.querySelectorAll('umbra-item-monitoramento')).toHaveLength(1);
    expect(document.activeElement).toBe(botao('Remover Black Myth: Wukong'));
  });
});
