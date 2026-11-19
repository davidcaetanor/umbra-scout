import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ITENS_NAVEGACAO } from '../../navegacao/itens-navegacao';

@Component({
  selector: 'umbra-barra-inferior',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './barra-inferior.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarraInferiorComponent {
  protected readonly itens = ITENS_NAVEGACAO;
}
