import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, output } from '@angular/core';
import { FrasesEmLinhasPipe } from '../../formatacao/frases-em-linhas.pipe';
import { MascoteComponent } from '../../mascote/mascote.component';
import { EstadoDoBloco } from './estado-do-bloco';

@Component({
  selector: 'umbra-estado-do-bloco',
  imports: [FrasesEmLinhasPipe, MascoteComponent],
  templateUrl: './estado-do-bloco.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block outline-none', tabindex: '-1' },
})
export class EstadoDoBlocoComponent {
  private readonly hospedeiro = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly estado = input.required<EstadoDoBloco<unknown>>();
  readonly tentarDeNovo = output();

  protected readonly mensagem = computed(() => {
    const atual = this.estado();
    return atual.tipo === 'erro' ? atual.mensagem : '';
  });

  protected pedirNovaTentativa(): void {
    this.hospedeiro.nativeElement.focus();
    this.tentarDeNovo.emit();
  }
}
