import { expect, test } from '@playwright/test';
import { API } from './apoio/sessao';

const MARCADOR = '<!-- configuracao -->';
const BLOCO = `<script id="configuracao" type="application/json">{"apiUrl":"${API}","loginGoogle":false}</script>`;

test.describe('Partida com a configuração embutida', () => {
  test('inicia sem pedir o config.json quando o index traz o bloco', async ({ page }) => {
    const pedidosDaConfiguracao: string[] = [];
    page.on('request', (pedido) => pedido.url().endsWith('/config.json') && pedidosDaConfiguracao.push(pedido.url()));
    await page.route(`${API}/**`, (pedido) => pedido.abort('connectionrefused'));
    await page.route('**/catalogo', async (pedido) => {
      const resposta = await pedido.fetch();
      const pagina = await resposta.text();
      expect(pagina).toContain(MARCADOR);
      await pedido.fulfill({ response: resposta, body: pagina.replace(MARCADOR, BLOCO) });
    });

    await page.goto('/catalogo');

    await expect(page.getByRole('heading', { level: 1, name: 'Catálogo' })).toBeVisible();
    expect(pedidosDaConfiguracao).toEqual([]);
  });

  test('busca o config.json quando o index não traz o bloco', async ({ page }) => {
    await page.route(`${API}/**`, (pedido) => pedido.abort('connectionrefused'));
    const configuracao = page.waitForRequest((pedido) => pedido.url().endsWith('/config.json'));

    await page.goto('/catalogo');

    await configuracao;
    await expect(page.getByRole('heading', { level: 1, name: 'Catálogo' })).toBeVisible();
  });
});
