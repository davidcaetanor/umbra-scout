import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MascoteComponent } from '../../../shared/mascote/mascote.component';

@Component({
  selector: 'umbra-pagina-nao-encontrada-page',
  imports: [MascoteComponent, RouterLink],
  templateUrl: './pagina-nao-encontrada.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginaNaoEncontradaPage {}
