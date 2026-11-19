import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BarraInferiorComponent } from '../barra-inferior/barra-inferior.component';
import { CabecalhoComponent } from '../cabecalho/cabecalho.component';
import { ID_CONTEUDO_PRINCIPAL } from '../../navegacao/foco-na-troca-de-rota';

@Component({
  selector: 'umbra-shell',
  imports: [RouterOutlet, CabecalhoComponent, BarraInferiorComponent],
  templateUrl: './shell.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected readonly idConteudo = ID_CONTEUDO_PRINCIPAL;
}
