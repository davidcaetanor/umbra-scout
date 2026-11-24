import { Page, expect, test } from '@playwright/test';
import { API, CHAVE_DO_TOKEN, comSessao, responderUsuario, tokenQueExpiraEm } from './apoio/sessao';

const OFERTA = {
  coletadoEm: '2026-11-24T10:00:00Z',
  verificadoEm: '2026-11-24T10:00:00Z',
  descontoPct: 30,
  expiraEm: null,
  gratuito: false,
  loja: 'Steam',
  situacao: 'vigente',
  url: 'https://loja.exemplo/hades',
  valorCentavos: 6990,
  valorOriginalCentavos: 9990,
};

const HADES = { gid: 'g-hades', nome: 'Hades II', imagemUrl: null, melhorPreco: OFERTA };

const LARGURAS = [
  { nome: 'minima', largura: 360, altura: 780 },
  { nome: 'maxima', largura: 1920, altura: 1080 },
] as const;

interface Simulacao {
  readonly criados: unknown[];
  respostas: { status: number; corpo: unknown; cabecalhos?: Record<string, string> }[];
}

async function simular(page: Page): Promise<Simulacao> {
  const simulacao: Simulacao = { criados: [], respostas: [] };
  await page.route(`${API}/produtos**`, (rota) => {
    const url = new URL(rota.request().url());
    const externos = url.searchParams.get('incluirExterno') === 'true' ? [HADES] : [];
    return rota.fulfill({ json: { conteudo: [], externos, pagina: 0, tamanho: 20, totalElementos: 0, totalPaginas: 0 } });
  });
  await page.route(`${API}/monitoramentos`, (rota) => {
    simulacao.criados.push(rota.request().postDataJSON());
    const resposta = simulacao.respostas.shift() ?? {
      status: 201,
      corpo: { id: 9, produtoId: 77, precoAlvoCentavos: 6000 },
    };
    return rota.fulfill({ status: resposta.status, json: resposta.corpo, headers: resposta.cabecalhos });
  });
  return simulacao;
}

async function medirLargura(page: Page) {
  return page.evaluate(() => {
    const visivel = document.documentElement.clientWidth;
    const vazando = [...document.querySelectorAll('body *')]
      .filter((el) => el.getBoundingClientRect().right > visivel + 0.5)
      .map((el) => el.tagName.toLowerCase());
    return { visivel, total: document.documentElement.scrollWidth, vazando };
  });
}

for (const { nome, largura, altura } of LARGURAS) {
  test.describe(`T4.6b em ${largura} px`, () => {
    test.use({ viewport: { width: largura, height: altura } });

    test('monitorar pelo card externo sem sair da busca', async ({ page }, info) => {
      await comSessao(page);
      const simulacao = await simular(page);
      await page.goto('/catalogo?nome=hades');

      const secao = page.getByRole('region', { name: 'Fora do catálogo' });
      await secao.getByRole('button', { name: /^Monitorar.*Hades II/ }).click();

      const campo = page.getByLabel('Preço alvo');
      await expect(campo).toBeFocused();
      const medida = await medirLargura(page);
      expect(medida.total, 'largura rolável igual à visível').toBe(medida.visivel);
      await info.attach(`${nome}-dialogo-externo`, { body: await page.screenshot(), contentType: 'image/png' });

      await campo.fill('60');
      await page.keyboard.press('Enter');

      await expect(secao.getByText('Monitorando, alvo')).toBeVisible();
      await expect(secao.getByRole('link', { name: /^Ver produto.*Hades II/ })).toBeFocused();
      await expect(page).toHaveURL(/\/catalogo\?nome=hades&fora=1$/);
      expect(simulacao.criados).toEqual([{ gid: 'g-hades', precoAlvoCentavos: 6000 }]);
      await info.attach(`${nome}-monitorando`, { body: await page.screenshot({ fullPage: true }), contentType: 'image/png' });
    });
  });
}

test.describe('T4.6b, regras', () => {
  test('visitante vai ao login, volta à busca com o diálogo preenchido e só grava ao confirmar', async ({ page }) => {
    await responderUsuario(page);
    const simulacao = await simular(page);
    await page.goto('/catalogo?nome=hades');

    await page.getByRole('button', { name: /^Monitorar.*Hades II/ }).click();
    await page.getByLabel('Preço alvo').fill('60');
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(/\/entrar\?returnUrl=/);
    expect(simulacao.criados).toEqual([]);
    const retorno = new URL(page.url()).searchParams.get('returnUrl')!;
    expect(retorno).toBe('/catalogo?nome=hades&fora=1');

    await page.evaluate(([chave, valor]) => localStorage.setItem(chave, valor), [CHAVE_DO_TOKEN, tokenQueExpiraEm(6 * 3600)]);
    await page.goto(retorno);

    await expect(page.getByLabel('Preço alvo')).toHaveValue('60,00');
    await expect(page.getByRole('button', { name: 'Monitorar', exact: true })).toBeFocused();
    expect(simulacao.criados).toEqual([]);

    await page.keyboard.press('Enter');
    await expect(page.getByText('Monitorando, alvo')).toBeVisible();
    expect(simulacao.criados).toEqual([{ gid: 'g-hades', precoAlvoCentavos: 6000 }]);
  });

  test('fonte ocupada mostra a espera e a nova tentativa conclui', async ({ page }) => {
    await comSessao(page);
    const simulacao = await simular(page);
    simulacao.respostas.push({
      status: 503,
      corpo: { status: 503, titulo: 'Serviço indisponível' },
      cabecalhos: { 'Retry-After': '60', 'Access-Control-Expose-Headers': 'Retry-After' },
    });
    await page.goto('/catalogo?nome=hades');

    await page.getByRole('button', { name: /^Monitorar.*Hades II/ }).click();
    await page.getByLabel('Preço alvo').fill('60');
    await page.keyboard.press('Enter');

    await expect(page.getByRole('alert')).toContainText('Tente de novo em 60 segundos.');
    await expect(page.getByLabel('Preço alvo')).toHaveValue('60');

    await page.getByRole('button', { name: 'Monitorar', exact: true }).click();
    await expect(page.getByText('Monitorando, alvo')).toBeVisible();
    expect(simulacao.criados).toHaveLength(2);
  });

  test('com filtro de hardware não há busca fora do catálogo', async ({ page }) => {
    await comSessao(page);
    const pedidos: string[] = [];
    await page.route(`${API}/produtos**`, (rota) => {
      pedidos.push(rota.request().url());
      return rota.fulfill({ json: { conteudo: [], externos: [], pagina: 0, tamanho: 20, totalElementos: 0, totalPaginas: 0 } });
    });
    await page.goto('/catalogo?nome=rtx&tipo=HARDWARE');

    await expect(page.getByRole('heading', { name: 'Nada com "rtx"' })).toBeVisible();
    await expect(page.getByRole('button', { name: /fora do catálogo/ })).toHaveCount(0);
    expect(pedidos.some((url) => url.includes('incluirExterno=true'))).toBe(false);
  });
});
