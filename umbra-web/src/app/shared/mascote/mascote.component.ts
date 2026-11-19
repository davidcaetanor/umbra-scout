import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DirecaoMascote, EstadoMascote, TamanhoMascote } from './estado-mascote';
import { ESCALA_POR_TAMANHO, poseDe } from './poses';

@Component({
  selector: 'umbra-mascote',
  templateUrl: './mascote.component.html',
  styleUrl: './mascote.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
})
export class MascoteComponent {
  readonly estado = input.required<EstadoMascote>();
  readonly tamanho = input<TamanhoMascote>('pequeno');
  readonly direcao = input<DirecaoMascote>('south');

  protected readonly pose = computed(() => poseDe(this.estado(), this.direcao()));
  protected readonly repete = computed(() => this.pose().repeticao === 'loop');

  protected readonly estilo = computed(() => {
    const pose = this.pose();
    const escala = ESCALA_POR_TAMANHO[this.tamanho()];
    const passos = this.repete() ? pose.quadros : pose.quadros - 1;
    return {
      '--sprite': `url(${pose.arquivo})`,
      '--largura': `${pose.larguraQuadro * escala}px`,
      '--altura': `${pose.alturaQuadro * escala}px`,
      '--altura-visivel': `${(pose.alturaQuadro - pose.sobraAbaixoDosPes) * escala}px`,
      '--largura-tira': `${pose.larguraQuadro * pose.quadros * escala}px`,
      '--destino': `${-pose.larguraQuadro * passos * escala}px`,
      '--passos': `${passos}`,
      '--duracao': `${pose.quadros / pose.fps}s`,
    };
  });
}
