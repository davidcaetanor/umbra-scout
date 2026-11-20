import { expect, test } from '@playwright/test';
import { API } from './apoio/sessao';

const OFERTA = {
  coletadoEm: '2026-11-19T10:00:00Z',
  descontoPct: 50,
  expiraEm: null,
  gratuito: false,
  loja: 'Nuuvem',
  promocaoEncerrada: false,
  url: 'https://loja.exemplo/jogo',
  valorCentavos: 2399,
  valorOriginalCentavos: 4799,
};

function produto(id: number, nome: string) {
  return { id, nome, tipo: 'JOGO', categoria: null, imagemUrl: null, melhorPreco: OFERTA };
}

test.describe('T4.4b, página inicial com Destaques Umbra', () => {
  test('mostra os destaques com a loja e leva ao catálogo de melhores ofertas', async ({ page }) => {
    await page.route(`${API}/produtos/destaques`, (rota) =>
      rota.fulfill({ json: { conteudo: [produto(1, 'Hades II'), produto(2, 'Celeste')] } }),
    );
    await page.route(`${API}/produtos?**`, (rota) =>
      rota.fulfill({ json: { conteudo: [], externos: [], pagina: 0, tamanho: 20, totalElementos: 0, totalPaginas: 0 } }),
    );

    await page.goto('/');

    await expect(page).toHaveTitle('Início · Umbra');
    await expect(page.getByRole('heading', { name: 'Destaques Umbra' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Hades II' })).toBeVisible();
    await expect(page.getByText('Nuuvem').first()).toBeVisible();

    await page.getByRole('link', { name: 'Ver todos no catálogo' }).click();
    await expect(page).toHaveURL(/\/catalogo\?tipo=JOGO&ordenacao=melhoresOfertas$/);
    await expect(page.getByRole('combobox', { name: 'Ordenar por' })).toHaveValue('melhoresOfertas');
  });

  test('a busca da inicial leva o termo ao catálogo', async ({ page }) => {
    await page.route(`${API}/produtos**`, (rota) =>
      rota.fulfill({
        json: rota.request().url().includes('/destaques')
          ? { conteudo: [] }
          : { conteudo: [produto(3, 'Hollow Knight')], externos: [], pagina: 0, tamanho: 20, totalElementos: 1, totalPaginas: 1 },
      }),
    );

    await page.goto('/');
    await page.getByRole('searchbox', { name: 'Buscar um jogo ou peça' }).fill('hollow');
    await page.getByRole('button', { name: 'Buscar' }).click();

    await expect(page).toHaveURL(/\/catalogo\?nome=hollow$/);
    await expect(page.getByRole('heading', { name: 'Hollow Knight' })).toBeVisible();
  });
});
