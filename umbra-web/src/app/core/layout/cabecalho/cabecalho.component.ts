import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ITENS_NAVEGACAO } from '../../navegacao/itens-navegacao';

@Component({
  selector: 'umbra-cabecalho',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './cabecalho.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CabecalhoComponent {
  protected readonly itens = ITENS_NAVEGACAO;
}
