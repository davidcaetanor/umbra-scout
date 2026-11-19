import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EntradasVistas } from './entradas-vistas';
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
  private readonly entradas = inject(EntradasVistas);
  private readonly documento = inject(DOCUMENT);

  readonly estado = input.required<EstadoMascote>();
  readonly tamanho = input<TamanhoMascote>('pequeno');
  readonly direcao = input<DirecaoMascote>('south');

  protected readonly pose = computed(() => poseDe(this.estado(), this.direcao()));
  protected readonly repete = computed(() => this.pose().repeticao === 'loop');

  private readonly terminou = signal(false);
  private readonly chaveDaEntrada = computed(() => `${this.documento.location?.pathname ?? ''}#${this.estado()}`);

  protected readonly emRepouso = computed(() => {
    if (this.repete()) {
      return false;
    }
    return this.terminou() || this.entradas.jaViu(this.chaveDaEntrada());
  });

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
      '--repouso': `${-pose.larguraQuadro * pose.quadroDeRepouso * escala}px`,
      '--passos': `${passos}`,
      '--duracao': `${pose.quadros / pose.fps}s`,
    };
  });

  protected aoTerminar(): void {
    this.terminou.set(true);
    this.entradas.registrar(this.chaveDaEntrada());
  }
}
