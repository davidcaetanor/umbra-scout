import { Page, expect, test } from '@playwright/test';

async function abrir(page: Page, caminho: string): Promise<void> {
  await page.goto(caminho);
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
}

test.describe('RNF-06, roteiro de teclado', () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test('a primeira tecla Tab mostra o link de salto, que leva ao conteúdo', async ({ page }) => {
    await abrir(page, '/catalogo');
    await page.keyboard.press('Tab');

    const salto = page.getByRole('link', { name: 'Pular para o conteúdo' });
    await expect(salto).toBeFocused();
    await expect(salto).toBeInViewport();

    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/#conteudo$/);
  });

  test('a ordem do foco segue a ordem visual do cabeçalho', async ({ page }) => {
    await abrir(page, '/catalogo');
    const esperados = ['Pular para o conteúdo', 'Umbra, página inicial', 'Catálogo', 'Meus monitoramentos', 'Entrar'];

    for (const nome of esperados) {
      await page.keyboard.press('Tab');
      await expect(page.locator(':focus')).toHaveAccessibleName(nome);
    }
  });

  test('o foco é visível com contorno de pelo menos 2 px', async ({ page }) => {
    await abrir(page, '/catalogo');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    const contorno = await page.locator(':focus').evaluate((el) => {
      const estilo = getComputedStyle(el);
      return { estilo: estilo.outlineStyle, largura: parseFloat(estilo.outlineWidth) };
    });
    expect(contorno.estilo).not.toBe('none');
    expect(contorno.largura).toBeGreaterThanOrEqual(2);
  });

  test('navegar pelo teclado leva o foco ao título da nova página e atualiza a aba', async ({ page }) => {
    await abrir(page, '/catalogo');
    await page.getByRole('link', { name: 'Meus monitoramentos' }).first().focus();
    await page.keyboard.press('Enter');

    await expect(page.getByRole('heading', { level: 1, name: 'Meus monitoramentos' })).toBeFocused();
    await expect(page).toHaveTitle('Meus monitoramentos · Umbra');
  });

  test('todo alvo interativo tem pelo menos 24 por 24 px', async ({ page }) => {
    await abrir(page, '/catalogo');
    const pequenos = await page.evaluate(() =>
      [...document.querySelectorAll('a[href], button')]
        .filter((el) => getComputedStyle(el).display !== 'none' && el.getBoundingClientRect().width > 1)
        .map((el) => ({ nome: el.textContent?.trim(), ...el.getBoundingClientRect().toJSON() }))
        .filter((r) => r.width < 24 || r.height < 24),
    );
    expect(pequenos).toEqual([]);
  });
});
