import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

const MARCA = 'Umbra';

@Injectable()
export class TituloComMarcaStrategy extends TitleStrategy {
  private readonly titulo = inject(Title);

  override updateTitle(estado: RouterStateSnapshot): void {
    const tituloDaRota = this.buildTitle(estado);
    this.titulo.setTitle(tituloDaRota ? `${tituloDaRota} · ${MARCA}` : MARCA);
  }
}
