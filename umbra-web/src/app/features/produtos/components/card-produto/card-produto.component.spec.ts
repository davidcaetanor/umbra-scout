import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProdutoExibido } from '../produto-exibido';
import { CardProdutoComponent } from './card-produto.component';

const PRODUTO: ProdutoExibido = {
  nome: 'Placa RTX',
  tipo: 'HARDWARE',
  capa: {
    endereco: '/imagens/itad/a/banner300.jpg',
    alternativas: '/imagens/itad/a/banner300.jpg 300w, /imagens/itad/a/banner400.jpg 400w',
    reserva: '/imagens/itad/a/boxart.jpg',
  },
  categoria: 'Placa de vídeo',
  preco: { atual: 'R$ 1.500,00', original: 'R$ 2.000,00', descontoPct: 25, loja: 'Kabum' },
};

describe('CardProdutoComponent', () => {
  let fixture: ComponentFixture<CardProdutoComponent>;
  let referencia: ComponentRef<CardProdutoComponent>;

  async function mostrar(produto: ProdutoExibido, prioritaria = false): Promise<HTMLElement> {
    referencia.setInput('produto', produto);
    referencia.setInput('prioritaria', prioritaria);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    fixture = TestBed.createComponent(CardProdutoComponent);
    referencia = fixture.componentRef;
  });

  const imagem = (card: HTMLElement) => card.querySelector('img');

  it('mostra nome, categoria, preco, desconto com texto e preco original', async () => {
    const card = await mostrar(PRODUTO);

    expect(card.querySelector('h3')?.textContent?.trim()).toBe('Placa RTX');
    expect(card.textContent).toContain('Placa de vídeo');
    expect(card.textContent).toContain('R$ 1.500,00');
    expect(card.textContent?.replace(/\s+/g, ' ')).toContain('-25% de desconto');
    expect(card.querySelector('s')?.textContent).toBe('R$ 2.000,00');
  });

  it('pede a faixa no tamanho da tela, decorativa, sem referer e com espaco reservado', async () => {
    const card = await mostrar(PRODUTO);
    const capa = imagem(card)!;

    expect(capa.getAttribute('src')).toBe('/imagens/itad/a/banner300.jpg');
    expect(capa.getAttribute('srcset')).toContain('banner400.jpg 400w');
    expect(capa.getAttribute('sizes')).toContain('120px');
    expect(capa.getAttribute('alt')).toBe('');
    expect(capa.getAttribute('referrerpolicy')).toBe('no-referrer');
    expect([capa.getAttribute('width'), capa.getAttribute('height')]).toEqual(['300', '140']);
  });

  it('so a primeira fileira carrega de imediato; o resto espera chegar perto da tela', async () => {
    expect(imagem(await mostrar(PRODUTO, true))!.getAttribute('loading')).toBe('eager');
    expect(imagem(await mostrar(PRODUTO, false))!.getAttribute('loading')).toBe('lazy');
  });

  it('entra transparente e aparece quando termina de carregar', async () => {
    const card = await mostrar(PRODUTO);

    expect(imagem(card)!.classList.contains('opacity-100')).toBe(false);
    imagem(card)!.dispatchEvent(new Event('load'));
    await fixture.whenStable();
    expect(imagem(card)!.classList.contains('opacity-100')).toBe(true);
  });

  it('faixa ausente cai na capa em pe, e sem nenhuma vira o marcador do tipo', async () => {
    const card = await mostrar(PRODUTO);

    imagem(card)!.dispatchEvent(new Event('error'));
    await fixture.whenStable();
    expect(imagem(card)!.getAttribute('src')).toBe('/imagens/itad/a/boxart.jpg');
    expect(imagem(card)!.hasAttribute('srcset')).toBe(false);

    imagem(card)!.dispatchEvent(new Event('error'));
    await fixture.whenStable();
    expect(imagem(card)).toBeNull();
    expect(card.querySelector('svg[aria-hidden="true"]')).not.toBeNull();
  });

  it('produto sem oferta diz que esta indisponivel e sem capa mostra o marcador', async () => {
    const card = await mostrar({ ...PRODUTO, capa: null, categoria: null, preco: null });

    expect(card.textContent).toContain('Indisponível agora');
    expect(imagem(card)).toBeNull();
  });
});
