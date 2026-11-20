import { TestBed } from '@angular/core/testing';
import { CapaExibida } from '../capa-exibida';
import { CapaProdutoComponent } from './capa-produto.component';

const FAIXA: CapaExibida = { endereco: '/imagens/itad/a/banner300.jpg', alternativas: '', reserva: null, enquadramento: 'faixa' };
const FOTO: CapaExibida = {
  endereco: '/imagens/kabum/produtos/fotos/1/1_index_g.jpg',
  alternativas: '',
  reserva: null,
  enquadramento: 'foto',
};

async function montar(capa: CapaExibida | null) {
  const fixture = TestBed.createComponent(CapaProdutoComponent);
  fixture.componentRef.setInput('capa', capa);
  fixture.componentRef.setInput('tipo', 'HARDWARE');
  await fixture.whenStable();
  const host = fixture.nativeElement as HTMLElement;
  return { host, imagem: host.querySelector('img') };
}

describe('CapaProdutoComponent', () => {
  it('faixa do jogo preenche o espaco, cortando as bordas', async () => {
    const { host, imagem } = await montar(FAIXA);

    expect(imagem?.classList).toContain('object-cover');
    expect(host.classList).not.toContain('bg-moldura-foto');
  });

  it('foto do hardware aparece inteira numa moldura clara', async () => {
    const { host, imagem } = await montar(FOTO);

    expect(imagem?.classList).toContain('object-contain');
    expect(imagem?.classList).not.toContain('object-cover');
    expect(host.classList).toContain('bg-moldura-foto');
    expect(host.classList).not.toContain('bg-superficie-elevada');
  });

  it('sem imagem mostra o icone do tipo, sem moldura', async () => {
    const { host, imagem } = await montar(null);

    expect(imagem).toBeNull();
    expect(host.querySelector('svg')).not.toBeNull();
    expect(host.classList).not.toContain('bg-moldura-foto');
  });

  it('foto que falha ao carregar perde a moldura e cai no icone', async () => {
    const fixture = TestBed.createComponent(CapaProdutoComponent);
    fixture.componentRef.setInput('capa', FOTO);
    fixture.componentRef.setInput('tipo', 'HARDWARE');
    await fixture.whenStable();
    const host = fixture.nativeElement as HTMLElement;

    host.querySelector('img')!.dispatchEvent(new Event('error'));
    await fixture.whenStable();

    expect(host.querySelector('img')).toBeNull();
    expect(host.classList).not.toContain('bg-moldura-foto');
  });
});
