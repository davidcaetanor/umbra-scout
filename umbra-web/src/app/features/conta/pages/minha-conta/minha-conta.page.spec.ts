import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ContaService } from '../../data/conta.service';
import { MinhaContaPage } from './minha-conta.page';

const DADOS = { nome: 'Ana Maria da Silva', email: 'ana@exemplo.com', regiao: null, membroDesde: '19 de novembro de 2026' };

describe('MinhaContaPage', () => {
  let fixture: ComponentFixture<MinhaContaPage>;
  let carregar: ReturnType<typeof vi.fn>;

  async function abrir(): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [MinhaContaPage],
      providers: [{ provide: ContaService, useValue: { carregar } }],
    });
    fixture = TestBed.createComponent(MinhaContaPage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const termos = (tela: HTMLElement): Record<string, string | undefined> =>
    Object.fromEntries(
      [...tela.querySelectorAll('dt')].map((dt) => [dt.textContent?.trim() ?? '', dt.nextElementSibling?.textContent?.trim()]),
    );

  it('mostra que esta carregando', async () => {
    carregar = vi.fn().mockReturnValue(new Promise(() => undefined));

    const tela = await abrir();

    expect(tela.querySelector('[role="status"]')?.textContent).toContain('Carregando seus dados');
  });

  it('mostra o nome completo e diz quando a regiao nao foi informada', async () => {
    carregar = vi.fn().mockResolvedValue(DADOS);

    const tela = await abrir();

    expect(termos(tela)).toEqual({
      Nome: 'Ana Maria da Silva',
      Email: 'ana@exemplo.com',
      Região: 'Não informada',
      'No Umbra desde': '19 de novembro de 2026',
    });
  });

  it('mostra a regiao quando existe', async () => {
    carregar = vi.fn().mockResolvedValue({ ...DADOS, regiao: 'São Paulo/SP' });

    expect(termos(await abrir())['Região']).toBe('São Paulo/SP');
  });

  it('na falha oferece tentar de novo e recarrega', async () => {
    carregar = vi.fn().mockRejectedValueOnce(new Error('rede')).mockResolvedValueOnce(DADOS);
    const tela = await abrir();

    expect(tela.querySelector('[role="alert"]')?.textContent).toContain('Não foi possível carregar seus dados agora.');

    tela.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    await fixture.whenStable();

    expect(carregar).toHaveBeenCalledTimes(2);
    expect(termos(tela)['Nome']).toBe('Ana Maria da Silva');
  });
});
