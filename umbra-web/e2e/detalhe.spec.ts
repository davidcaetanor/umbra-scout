import { Page, expect, test } from '@playwright/test';
import { API, comSessao } from './apoio/sessao';

const DIA = 86_400_000;
const AGORA = Date.parse('2026-11-21T12:00:00Z');
const instante = (dias: number) => new Date(AGORA - dias * DIA).toISOString();

function oferta(loja: string, valorCentavos: number) {
  return {
    coletadoEm: '2026-11-21T10:00:00Z',
    descontoPct: 20,
    expiraEm: null,
    gratuito: false,
    loja,
    promocaoEncerrada: false,
    url: `https://loja.exemplo/${loja}`,
    valorCentavos,
    valorOriginalCentavos: 9000,
  };
}

const PRODUTO = {
  id: 42,
  nome: 'Hades II',
  tipo: 'JOGO',
  categoria: null,
  imagemUrl: null,
  melhorPreco: oferta('GOG', 7240),
  ofertas: [oferta('GOG', 7240), oferta('Steam', 8000)],
};

const HISTORICO = {
  consultadoEm: new Date(AGORA).toISOString(),
  observadoDesde: instante(120),
  periodo: '90d',
  faixas: [],
  melhorPreco: [
    { instante: instante(80), valorCentavos: 9000 },
    { instante: instante(40), valorCentavos: 6500 },
    { instante: instante(30), valorCentavos: 8000 },
    { instante: instante(5), valorCentavos: 7240 },
  ],
  porLoja: [
    { loja: 'GOG', pontos: [{ instante: instante(80), valorCentavos: 9000 }] },
    { loja: 'Steam', pontos: [{ instante: instante(80), valorCentavos: 9500 }] },
  ],
  minimo: { valorCentavos: 6500, gratuito: false, loja: 'GOG', em: instante(40) },
  variacao: { diferencaCentavos: 740, percentualAcimaDoMinimo: 11, precoAtualCentavos: 7240 },
};

function paginaDeMonitoramentos(conteudo: unknown[]) {
  return { conteudo, pagina: 0, tamanho: 1, totalElementos: conteudo.length, totalPaginas: 1 };
}

async function simularProduto(page: Page): Promise<void> {
  await page.route(`${API}/produtos/42`, (rota) => rota.fulfill({ json: PRODUTO }));
  await page.route(`${API}/produtos/42/historico**`, (rota) => rota.fulfill({ json: HISTORICO }));
}

async function simularMonitoramentos(page: Page, existente: unknown[] = [], criar: { status: number; json: unknown } | null = null) {
  await page.route(`${API}/monitoramentos**`, (rota) => {
    if (rota.request().method() === 'POST' && criar !== null) {
      return rota.fulfill(criar);
    }
    return rota.fulfill({ json: paginaDeMonitoramentos(existente) });
  });
}

test.describe('T4.5, detalhe do produto', () => {
  test('o card do catálogo leva ao detalhe, com hover e cursor de mão', async ({ page }) => {
    await page.route(`${API}/produtos?**`, (rota) =>
      rota.fulfill({
        json: { conteudo: [{ ...PRODUTO, ofertas: undefined }], externos: [], pagina: 0, tamanho: 20, totalElementos: 1, totalPaginas: 1 },
      }),
    );
    await simularProduto(page);
    await page.goto('/catalogo');

    const link = page.getByRole('link', { name: 'Hades II' });
    const card = page.locator('umbra-card-produto article');
    const bordaAntes = await card.evaluate((el) => getComputedStyle(el).borderColor);
    await card.hover({ position: { x: 20, y: 20 } });
    await expect.poll(() => card.evaluate((el) => getComputedStyle(el).borderColor)).not.toBe(bordaAntes);
    expect(await link.evaluate((el) => getComputedStyle(el).cursor)).toBe('pointer');

    await card.click({ position: { x: 20, y: 20 } });
    await expect(page).toHaveURL(/\/catalogo\/42$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Hades II' })).toBeVisible();
    await expect(page).toHaveTitle('Hades II · Umbra');
    await expect(page.getByRole('link', { name: 'Catálogo', exact: true }).first()).toHaveClass(/ativo/);
  });

  test('mostra gráfico com resumo, legenda, períodos e tabela por loja', async ({ page }) => {
    await simularProduto(page);
    await page.goto('/catalogo/42');

    await expect(page.getByRole('img', { name: /Histórico de preço em 90 dias, menor registrado R\$ 65,00/ })).toBeVisible();
    const legenda = page.getByRole('list', { name: 'Legenda do histórico' });
    await expect(legenda).toContainText('Hoje, 21/11');
    await expect(legenda).toContainText('Melhor preço do dia');
    await expect(legenda).toContainText('Menor registrado');
    await expect(legenda).not.toContainText('Alvo de busca');
    await expect(page.getByRole('button', { name: '1 ano' })).toBeEnabled();
    await expect(page.getByRole('button', { name: '90 dias' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByRole('row', { name: /GOG/ })).toContainText('Mais barato');
  });

  test('deslogado vai ao login e volta com o alvo preenchido e o foco no botão', async ({ page }) => {
    await simularProduto(page);
    await simularMonitoramentos(page);
    await page.goto('/catalogo/42');

    await page.getByLabel('Preço alvo').fill('60');
    await page.getByRole('button', { name: 'Monitorar' }).click();
    await expect(page).toHaveURL(/\/entrar\?returnUrl=%2Fcatalogo%2F42$/);

    await comSessao(page);
    await page.goto('/catalogo/42');
    await expect(page.getByLabel('Preço alvo')).toHaveValue('60,00');
    await expect(page.getByRole('button', { name: 'Monitorar' })).toBeFocused();
  });

  test('logado cria o monitoramento, recebe o aviso e vê o alvo no gráfico', async ({ page }) => {
    await comSessao(page);
    await simularProduto(page);
    await simularMonitoramentos(page, [], { status: 201, json: { id: 9, produtoId: 42, precoAlvoCentavos: 6000 } });
    await page.goto('/catalogo/42');

    await page.getByLabel('Preço alvo').fill('60');
    await page.keyboard.press('Enter');

    await expect(page.getByRole('status').filter({ hasText: 'Monitoramento criado' })).toBeVisible();
    await expect(page.getByText('Você já monitora este produto com alvo de')).toBeVisible();
    await expect(page.getByRole('list', { name: 'Legenda do histórico' })).toContainText('Alvo de busca');
  });

  test('alvo acima de R$ 5 mil recebe piada do Umbra; acima de R$ 150 mil é recusado', async ({ page }, info) => {
    await comSessao(page);
    await simularProduto(page);
    await simularMonitoramentos(page, [], { status: 201, json: { id: 9, produtoId: 42, precoAlvoCentavos: 12_000_000 } });
    await page.goto('/catalogo/42');

    await page.getByLabel('Preço alvo').fill('150.000,01');
    await page.getByRole('button', { name: 'Monitorar' }).click();
    await expect(page.getByRole('alert')).toHaveText('O alvo vai até R$ 150.000,00.');

    await page.getByLabel('Preço alvo').fill('120.000');
    await page.getByRole('button', { name: 'Monitorar' }).click();
    const aviso = page.getByRole('status').filter({ hasText: 'Alvo salvo.' });
    await expect(aviso).toContainText('em promoção?');
    await info.attach('piada', { body: await page.screenshot(), contentType: 'image/png' });
  });

  test('quem já monitora vê o alvo; conflito ao criar leva a Meus monitoramentos', async ({ page }) => {
    await comSessao(page);
    await simularProduto(page);
    await simularMonitoramentos(page, [{ id: 9, produtoId: 42, precoAlvoCentavos: 6000 }]);
    await page.goto('/catalogo/42');
    await expect(page.getByText('R$ 60,00').first()).toBeVisible();

    await page.unroute(`${API}/monitoramentos**`);
    await simularMonitoramentos(page, [], {
      status: 409,
      json: { type: 'about:blank', title: 'Conflito', status: 409, detail: 'Você já monitora este produto.' },
    });
    await page.reload();
    await page.getByLabel('Preço alvo').fill('60');
    await page.getByRole('button', { name: 'Monitorar' }).click();
    await expect(page).toHaveURL(/\/monitoramentos$/);
  });

  for (const { largura, altura } of [
    { largura: 360, altura: 780 },
    { largura: 1920, altura: 1080 },
  ]) {
    test(`RNF-08 em ${largura} px, sem rolagem horizontal`, async ({ page }, info) => {
      await page.setViewportSize({ width: largura, height: altura });
      await simularProduto(page);
      await page.goto('/catalogo/42');
      await expect(page.getByRole('img', { name: /Histórico de preço/ })).toBeVisible();
      await page.getByRole('button', { name: 'Por loja' }).click();

      const medida = await page.evaluate(() => ({
        visivel: document.documentElement.clientWidth,
        total: document.documentElement.scrollWidth,
      }));
      expect(medida.total).toBe(medida.visivel);
      await info.attach(`detalhe-${largura}`, { body: await page.screenshot({ fullPage: true }), contentType: 'image/png' });
    });
  }

  test('o histórico é operável pelo teclado: período e por loja', async ({ page }) => {
    await simularProduto(page);
    await page.goto('/catalogo/42');
    await expect(page.getByRole('img', { name: /Histórico de preço/ })).toBeVisible();

    await page.getByRole('button', { name: '30 dias' }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('button', { name: '30 dias' })).toHaveAttribute('aria-pressed', 'true');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await expect(page.locator(':focus')).toHaveAccessibleName('Por loja');
    await page.keyboard.press('Space');
    await expect(page.getByRole('checkbox', { name: 'GOG' })).toBeChecked();
  });
});
