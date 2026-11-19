import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EstadoDoBloco } from './estado-do-bloco';
import { EstadoDoBlocoComponent } from './estado-do-bloco.component';

@Component({
  imports: [EstadoDoBlocoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <umbra-estado-do-bloco [estado]="estado()" (tentarDeNovo)="tentativas = tentativas + 1">
      <p carregando>Carregando a lista…</p>
      <p vazio>Nada por aqui.</p>
      <p>Lista pronta.</p>
    </umbra-estado-do-bloco>
  `,
})
class TelaDeTeste {
  readonly estado = signal<EstadoDoBloco<string>>({ tipo: 'carregando' });
  tentativas = 0;
}

describe('EstadoDoBlocoComponent', () => {
  let fixture: ComponentFixture<TelaDeTeste>;

  async function mostrar(estado: EstadoDoBloco<string>): Promise<HTMLElement> {
    fixture.componentInstance.estado.set(estado);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    fixture = TestBed.createComponent(TelaDeTeste);
  });

  it('carregando anuncia o rotulo da tela', async () => {
    const tela = await mostrar({ tipo: 'carregando' });

    expect(tela.querySelector('[role="status"]')?.textContent?.trim()).toBe('Carregando a lista…');
    expect(tela.querySelector('umbra-mascote')).toBeNull();
  });

  it('vazio e sucesso mostram so o conteudo de cada um', async () => {
    expect((await mostrar({ tipo: 'vazio' })).textContent?.trim()).toBe('Nada por aqui.');
    expect((await mostrar({ tipo: 'sucesso', dados: 'x' })).textContent?.trim()).toBe('Lista pronta.');
  });

  it('erro mostra o Umbra em erro, a mensagem uma frase por linha e tentar de novo', async () => {
    const tela = await mostrar({ tipo: 'erro', mensagem: 'Primeira frase. Segunda frase.' });
    const alerta = tela.querySelector('[role="alert"]');

    expect(alerta?.querySelector('p')?.textContent).toBe('Primeira frase.\nSegunda frase.');
    expect(alerta?.querySelector('umbra-mascote')).not.toBeNull();
    expect(alerta?.querySelector('button')?.textContent?.trim()).toBe('Tentar de novo');
  });

  it('tentar de novo avisa a tela e guarda o foco no bloco', async () => {
    const tela = await mostrar({ tipo: 'erro', mensagem: 'Falhou.' });

    tela.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();

    expect(fixture.componentInstance.tentativas).toBe(1);
    expect(document.activeElement).toBe(tela.querySelector('umbra-estado-do-bloco'));
  });
});
