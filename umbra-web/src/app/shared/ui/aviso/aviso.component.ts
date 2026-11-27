import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { caminhoDoRetrato } from '../../mascote/caminhos-da-arte';
import { EstadoMascote } from '../../mascote/estado-mascote';
import { MascoteComponent } from '../../mascote/mascote.component';
import { Aviso, AvisoService, RetratoDoAviso } from './aviso.service';

@Component({
  selector: 'umbra-aviso',
  imports: [MascoteComponent],
  templateUrl: './aviso.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'pointer-events-none fixed inset-x-4 bottom-24 z-30 flex justify-center tablet:inset-x-auto tablet:end-6 tablet:bottom-6',
  },
})
export class AvisoComponent {
  protected readonly avisos = inject(AvisoService);

  protected retratoDe(aviso: Aviso): string {
    return caminhoDoRetrato(this.nomeDoRetrato(aviso));
  }

  private nomeDoRetrato(aviso: Aviso): RetratoDoAviso {
    return aviso.figura.tipo === 'retrato' ? aviso.figura.retrato : 'feliz';
  }

  protected estadoDe(aviso: Aviso): EstadoMascote {
    return aviso.figura.tipo === 'sprite' ? aviso.figura.estado : 'comemorando';
  }
}
