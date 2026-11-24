import { Page, expect, test } from '@playwright/test';
import { API, comSessao } from './apoio/sessao';

const DIA = 86_400_000;
const AGORA = Date.parse('2026-11-21T12:00:00Z');
const instante = (dias: number) => new Date(AGORA - dias * DIA).toISOString();

function oferta(loja: string, valorCentavos: number) {
  return {
    coletadoEm: '2026-11-21T10:00:00Z',
    verificadoEm: '2026-11-21T10:00:00Z',
    descontoPct: 20,
    expiraEm: null,
    gratuito: false,
    loja,
    situacao: 'vigente',
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
    { loja: 'GOG', pontos: [{ instante: instante(80), valorCentavos: 9000 }, { instante: instante(40), valorCentavos: 6500 }] },
    { loja: 'Steam', pontos: [{ instante: instante(80), valorCentavos: 9500 }, { instante: instante(5), valorCentavos: 8000 }] },
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

  test('quem monitora altera o alvo no detalhe e o gráfico acompanha', async ({ page }) => {
    await comSessao(page);
    await simularProduto(page);
    await simularMonitoramentos(page, [{ id: 9, produtoId: 42, precoAlvoCentavos: 6000 }]);
    let corpo: unknown = null;
    await page.route(`${API}/monitoramentos/9`, (rota) => {
      corpo = rota.request().postDataJSON();
      return rota.fulfill({ json: { id: 9, produtoId: 42, precoAlvoCentavos: 5500 } });
    });
    await page.goto('/catalogo/42');

    await page.getByRole('button', { name: 'Alterar alvo' }).click();
    const campo = page.getByLabel('Preço alvo');
    await expect(campo).toHaveValue('60,00');
    await expect(campo).toBeFocused();
    await campo.fill('55.00');
    await page.keyboard.press('Enter');

    await expect(page.getByRole('status').filter({ hasText: 'Alvo atualizado' })).toBeVisible();
    expect(corpo).toEqual({ precoAlvoCentavos: 5500 });
    await expect(page.getByText('Você já monitora este produto com alvo de')).toContainText('R$ 55,00');
    await expect(page.getByRole('list', { name: 'Legenda do histórico' })).toContainText('R$ 55,00');
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
      await page.getByRole('button', { name: 'Comparar lojas' }).click();

      const medida = await page.evaluate(() => ({
        visivel: document.documentElement.clientWidth,
        total: document.documentElement.scrollWidth,
      }));
      expect(medida.total).toBe(medida.visivel);
      await info.attach(`detalhe-${largura}`, { body: await page.screenshot({ fullPage: true }), contentType: 'image/png' });
    });
  }

  test('o histórico é operável pelo teclado: período e comparar lojas', async ({ page }) => {
    await simularProduto(page);
    await page.goto('/catalogo/42');
    await expect(page.getByRole('img', { name: /Histórico de preço/ })).toBeVisible();

    await page.getByRole('button', { name: '30 dias' }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('button', { name: '30 dias' })).toHaveAttribute('aria-pressed', 'true');

    await page.getByRole('button', { name: 'Comparar lojas' }).focus();
    await page.keyboard.press('Enter');
    const resumo = page.getByRole('list', { name: 'Legenda do histórico' });
    await expect(resumo).toContainText('GOG');
    await expect(resumo).toContainText('Steam');
    await expect(page.getByRole('button', { name: 'Ver o melhor preço' })).toBeFocused();
  });

  test('a lista de mudanças conta o que aconteceu, sem texto de período indisponível', async ({ page }) => {
    await simularProduto(page);
    await page.goto('/catalogo/42');

    const lista = page.getByRole('list', { name: 'Mudanças de preço' });
    await expect(lista.getByRole('listitem').first()).toContainText('Steam caiu para R$ 80,00 (-16%)');
    await expect(page.getByText('disponível a partir de')).toHaveCount(0);
    await expect(page.getByRole('table').getByText(/Verificado/)).toHaveCount(0);
  });

  test('a tabela por loja mostra o prazo da promoção e os avisos, sem a hora da verificação', async ({ page }) => {
    const situacoes = [
      { loja: 'Steam', situacao: 'vigente', expiraEm: null, verificadoEm: '2026-11-21T10:00:00Z' },
      { loja: 'Epic', situacao: 'promocao-no-prazo', expiraEm: '2030-12-01T16:00:00Z', verificadoEm: '2026-11-19T12:00:00Z' },
      { loja: 'GOG', situacao: 'verificacao-atrasada', expiraEm: null, verificadoEm: '2026-11-10T12:00:00Z' },
      { loja: 'Humble', situacao: 'promocao-vencida-sem-coleta', expiraEm: '2026-11-20T15:00:00Z', verificadoEm: '2026-11-19T12:00:00Z' },
    ];
    const ofertas = situacoes.map((estado, indice) => ({ ...oferta(estado.loja, 7000 + indice), ...estado }));
    await page.route(`${API}/produtos/42`, (rota) => rota.fulfill({ json: { ...PRODUTO, melhorPreco: ofertas[0], ofertas } }));
    await page.route(`${API}/produtos/42/historico**`, (rota) => rota.fulfill({ json: HISTORICO }));
    await page.goto('/catalogo/42');

    const tabela = page.getByRole('table');
    await expect(tabela.getByText(/Verificado|última verificação/)).toHaveCount(0);
    await expect(tabela.getByText('Promoção até 01/12', { exact: true })).toBeVisible();
    await expect(tabela.getByText('Última verificação em 10/11, pode estar desatualizado')).toBeVisible();
    await expect(tabela.getByText('Promoção terminou em 20/11, aguardando nova verificação')).toBeVisible();
  });
});
