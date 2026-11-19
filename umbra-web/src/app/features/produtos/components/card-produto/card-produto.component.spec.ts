import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProdutoExibido } from '../produto-exibido';
import { CardProdutoComponent } from './card-produto.component';

const PRODUTO: ProdutoExibido = {
  nome: 'Placa RTX',
  tipo: 'HARDWARE',
  imagem: '/imagens/itad/a/boxart.jpg',
  categoria: 'Placa de vídeo',
  preco: { atual: 'R$ 1.500,00', original: 'R$ 2.000,00', descontoPct: 25, loja: 'Kabum' },
};

describe('CardProdutoComponent', () => {
  let fixture: ComponentFixture<CardProdutoComponent>;
  let referencia: ComponentRef<CardProdutoComponent>;

  async function mostrar(produto: ProdutoExibido): Promise<HTMLElement> {
    referencia.setInput('produto', produto);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    fixture = TestBed.createComponent(CardProdutoComponent);
    referencia = fixture.componentRef;
  });

  it('mostra nome, categoria, preco, desconto com texto e preco original', async () => {
    const card = await mostrar(PRODUTO);

    expect(card.querySelector('h3')?.textContent?.trim()).toBe('Placa RTX');
    expect(card.textContent).toContain('Placa de vídeo');
    expect(card.textContent).toContain('R$ 1.500,00');
    expect(card.textContent?.replace(/\s+/g, ' ')).toContain('-25% de desconto');
    expect(card.querySelector('s')?.textContent).toBe('R$ 2.000,00');
    expect(card.textContent).toContain('Kabum');
  });

  it('imagem decorativa, preguicosa e sem referer; na falha troca pelo marcador do tipo', async () => {
    const card = await mostrar(PRODUTO);
    const imagem = card.querySelector('img')!;

    expect(imagem.getAttribute('alt')).toBe('');
    expect(imagem.getAttribute('loading')).toBe('lazy');
    expect(imagem.getAttribute('referrerpolicy')).toBe('no-referrer');

    imagem.dispatchEvent(new Event('error'));
    await fixture.whenStable();

    expect(card.querySelector('img')).toBeNull();
    expect(card.querySelector('svg[aria-hidden="true"]')).not.toBeNull();
  });

  it('produto sem oferta diz que esta indisponivel', async () => {
    const card = await mostrar({ ...PRODUTO, imagem: null, categoria: null, preco: null });

    expect(card.textContent).toContain('Indisponível agora');
    expect(card.querySelector('img')).toBeNull();
  });
});
