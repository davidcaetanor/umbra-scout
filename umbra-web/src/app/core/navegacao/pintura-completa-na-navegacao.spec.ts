import { ApplicationRef, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { providePinturaCompletaNaNavegacao } from './pintura-completa-na-navegacao';

@Component({ template: '' })
class PaginaVazia {}

describe('providePinturaCompletaNaNavegacao', () => {
  it('verifica a aplicação logo depois de ativar a rota, antes da próxima pintura', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'catalogo', component: PaginaVazia }]), providePinturaCompletaNaNavegacao()],
    });
    const verificar = vi.spyOn(TestBed.inject(ApplicationRef), 'tick');

    await TestBed.inject(Router).navigateByUrl('/catalogo');
    await Promise.resolve();

    expect(verificar).toHaveBeenCalled();
  });
});
