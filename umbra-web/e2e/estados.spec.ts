import { expect, test } from '@playwright/test';
import { API } from './apoio/sessao';

const PAGINA = { conteudo: [], externos: [], pagina: 0, tamanho: 20, totalElementos: 0, totalPaginas: 0 };

test.describe('T4.4d, padrão de estados', () => {
  test.beforeEach(async ({ page }) => {
    await page.route(`${API}/produtos**`, (rota) => rota.fulfill({ json: PAGINA }));
  });

  for (const caminho of ['/catalogo', '/entrar', '/cadastro', '/rota-que-nao-existe']) {
    test(`${caminho}: todo botão e link habilitado mostra o cursor de mão`, async ({ page }) => {
      await page.goto(caminho);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

      const semMao = await page.evaluate(() =>
        [...document.querySelectorAll<HTMLElement>('button:not(:disabled), a[href], select')]
          .filter((elemento) => elemento.getClientRects().length > 0)
          .filter((elemento) => getComputedStyle(elemento).cursor !== 'pointer')
          .map((elemento) => elemento.textContent?.trim() || elemento.outerHTML.slice(0, 60)),
      );

      expect(semMao).toEqual([]);
    });
  }

  test('chip selecionado leva o check além da cor, e o hover do secundário muda o fundo', async ({ page }) => {
    await page.goto('/catalogo');

    const todos = page.getByRole('button', { name: 'Todos' });
    await expect(todos).toHaveAttribute('aria-pressed', 'true');
    await expect(todos.locator('svg')).toHaveCount(1);
    await expect(page.getByRole('button', { name: 'Jogos' }).locator('svg')).toHaveCount(0);

    const limpar = page.getByRole('button', { name: 'Limpar filtros' });
    const antes = await limpar.evaluate((botao) => getComputedStyle(botao).backgroundColor);
    await limpar.hover();
    await expect.poll(() => limpar.evaluate((botao) => getComputedStyle(botao).backgroundColor)).not.toBe(antes);
  });
});
