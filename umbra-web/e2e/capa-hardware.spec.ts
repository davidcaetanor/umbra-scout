import { expect, test } from '@playwright/test';
import { API } from './apoio/sessao';

const FOTO_DA_KABUM = 'https://images.kabum.com.br/produtos/fotos/85948/85948_index_g.jpg';

test('T4.4c, foto do hardware aparece inteira, na moldura clara e pela nossa origem', async ({ page }) => {
  await page.route(`${API}/produtos?**`, (rota) =>
    rota.fulfill({
      json: {
        conteudo: [
          {
            id: 1,
            nome: 'SSD Kingston A400 480GB',
            tipo: 'HARDWARE',
            categoria: 'SSD',
            imagemUrl: FOTO_DA_KABUM,
            melhorPreco: {
              coletadoEm: '2026-11-20T10:00:00Z',
              descontoPct: 10,
              expiraEm: null,
              gratuito: false,
              loja: 'KaBuM!',
              promocaoEncerrada: false,
              url: 'https://www.kabum.com.br/produto/85948',
              valorCentavos: 23999,
              valorOriginalCentavos: 26666,
            },
          },
        ],
        externos: [],
        pagina: 0,
        tamanho: 20,
        totalElementos: 1,
        totalPaginas: 1,
      },
    }),
  );

  await page.goto('/catalogo?tipo=HARDWARE');
  const imagem = page.locator('umbra-capa-produto img');

  await expect(imagem).toHaveAttribute('src', '/imagens/kabum/produtos/fotos/85948/85948_index_g.jpg');
  await expect(imagem).toHaveClass(/object-contain/);
  await expect(page.locator('umbra-capa-produto')).toHaveClass(/bg-moldura-foto/);
  await expect.poll(() => imagem.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBeGreaterThan(0);
  await page.locator('umbra-card-produto').screenshot({ path: 'e2e/resultados/capa-hardware.png' });
});
