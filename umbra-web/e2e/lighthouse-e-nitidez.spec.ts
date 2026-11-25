import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { ConsoleMessage, expect, test } from '@playwright/test';
import { API, comSessao } from './apoio/sessao';

const CAPA_RESERVA = readFileSync(resolve(__dirname, '../../deploy/capa-reserva.png'));

const OFERTA = {
  coletadoEm: '2026-11-24T10:00:00Z',
  verificadoEm: '2026-11-24T10:00:00Z',
  descontoPct: null,
  expiraEm: null,
  gratuito: false,
  loja: 'Steam',
  situacao: 'vigente',
  url: 'https://loja.exemplo/jogo',
  valorCentavos: 6990,
  valorOriginalCentavos: null,
};

const SEM_CAPA_NA_LOJA = 'https://assets.isthereanydeal.com/018df2a5/boxart.jpg';

function errosDe(mensagens: ConsoleMessage[]): string[] {
  return mensagens.filter((mensagem) => mensagem.type() === 'error').map((mensagem) => mensagem.text());
}

test.describe('T4.6c, Lighthouse e nitidez', () => {
  test('robots.txt tem exatamente as duas linhas', async ({ request }) => {
    const resposta = await request.get('/robots.txt');

    expect(resposta.headers()['content-type']).toContain('text/plain');
    expect((await resposta.text()).replace(/\r/g, '')).toBe('User-agent: *\nAllow: /\n');
  });

  test('rota privada leva noindex e a pública não', async ({ page }) => {
    const robots = page.locator('meta[name="robots"]');

    await page.goto('/entrar');
    await expect(robots).toHaveAttribute('content', 'noindex');

    await page.route(`${API}/produtos**`, (rota) =>
      rota.fulfill({ json: { conteudo: [], externos: [], pagina: 0, tamanho: 20, totalElementos: 0, totalPaginas: 0 } }),
    );
    await page.goto('/catalogo');
    await expect(robots).toHaveCount(0);
  });

  test('catálogo com capa ausente na loja mostra a reserva, direto e sem erro no console', async ({ page }) => {
    const mensagens: ConsoleMessage[] = [];
    page.on('console', (mensagem) => mensagens.push(mensagem));
    await page.route('**/imagens/**', (rota) => rota.fulfill({ status: 200, contentType: 'image/png', body: CAPA_RESERVA }));
    await page.route(`${API}/produtos**`, (rota) =>
      rota.fulfill({
        json: {
          conteudo: [{ id: 1, nome: 'Jogo sem capa', tipo: 'JOGO', categoria: null, imagemUrl: SEM_CAPA_NA_LOJA, melhorPreco: OFERTA }],
          externos: [],
          pagina: 0,
          tamanho: 20,
          totalElementos: 1,
          totalPaginas: 1,
        },
      }),
    );

    await page.goto('/catalogo');
    const capa = page.locator('umbra-capa-produto img');
    await expect(capa).toHaveJSProperty('complete', true);

    expect(await capa.evaluate((imagem) => imagem.naturalWidth)).toBeGreaterThan(0);
    expect(await capa.evaluate((imagem) => getComputedStyle(imagem).transitionDuration)).toBe('0s');
    expect(await capa.evaluate((imagem) => getComputedStyle(imagem).opacity)).toBe('1');
    expect(errosDe(mensagens)).toEqual([]);
  });

  test('carimbo inclina só a moldura, com o texto reto', async ({ page }) => {
    await comSessao(page);
    await page.route(`${API}/monitoramentos?**`, (rota) =>
      rota.fulfill({
        json: {
          conteudo: [
            {
              id: 1,
              produtoId: 10,
              nome: 'Hollow Knight: Silksong',
              imagemUrl: null,
              precoAlvoCentavos: 8000,
              ativo: true,
              criadoEm: '2026-11-20T10:00:00Z',
              melhorPreco: OFERTA,
              distanciaDoAlvoCentavos: -1010,
              alvoAtingido: true,
              aparentementeDescontinuado: false,
            },
          ],
          pagina: 0,
          tamanho: 20,
          totalElementos: 1,
          totalPaginas: 1,
        },
      }),
    );

    await page.goto('/monitoramentos');
    const carimbo = page.locator('.carimbo').first();
    await expect(carimbo).toHaveText('Alvo atingido');

    expect(await carimbo.evaluate((elemento) => getComputedStyle(elemento).transform)).toBe('none');
    expect(await carimbo.evaluate((elemento) => getComputedStyle(elemento, '::before').transform)).not.toBe('none');
  });
});
