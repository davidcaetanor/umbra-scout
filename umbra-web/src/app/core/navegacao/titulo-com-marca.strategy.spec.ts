import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot } from '@angular/router';
import { TituloComMarcaStrategy } from './titulo-com-marca.strategy';

describe('TituloComMarcaStrategy', () => {
  let estrategia: TituloComMarcaStrategy;
  let titulo: Title;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [TituloComMarcaStrategy] });
    estrategia = TestBed.inject(TituloComMarcaStrategy);
    titulo = TestBed.inject(Title);
  });

  it('acrescenta a marca ao título da rota', () => {
    vi.spyOn(estrategia, 'buildTitle').mockReturnValue('Catálogo');
    estrategia.updateTitle({} as RouterStateSnapshot);
    expect(titulo.getTitle()).toBe('Catálogo · Umbra');
  });

  it('usa só a marca quando a rota não tem título', () => {
    vi.spyOn(estrategia, 'buildTitle').mockReturnValue(undefined);
    estrategia.updateTitle({} as RouterStateSnapshot);
    expect(titulo.getTitle()).toBe('Umbra');
  });
});
