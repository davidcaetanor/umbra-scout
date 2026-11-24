import { Page, Request, expect, test } from '@playwright/test';
import { API } from './apoio/sessao';

const OFERTA = {
  coletadoEm: '2026-11-19T10:00:00Z',
  descontoPct: 50,
  expiraEm: null,
  gratuito: false,
  loja: 'Steam',
  situacao: 'vigente',
  url: 'https://loja.exemplo/jogo',
  valorCentavos: 2399,
  valorOriginalCentavos: 4799,
};

function produto(id: number, nome: string) {
  return { id, nome, tipo: 'JOGO', categoria: null, imagemUrl: null, melhorPreco: OFERTA };
}

function pagina(conteudo: unknown[], externos: unknown[] = [], totalPaginas = 1, numero = 0) {
  return { conteudo, externos, pagina: numero, tamanho: 20, totalElementos: conteudo.length, totalPaginas };
}

async function simularCatalogo(page: Page, responder: (url: URL) => unknown): Promise<Request[]> {
  const pedidos: Request[] = [];
  await page.route(`${API}/produtos**`, (rota) => {
    pedidos.push(rota.request());
    return rota.fulfill({ json: responder(new URL(rota.request().url())) });
  });
  return pedidos;
}

test.describe('T4.4, catálogo e busca', () => {
  test('digitar um nome gera uma chamada só, mantém o foco e não empilha histórico', async ({ page }) => {
    const pedidos = await simularCatalogo(page, (url) =>
      pagina(url.searchParams.has('nome') ? [produto(1, 'Hollow Knight')] : [produto(2, 'Celeste')]),
    );
    await page.goto('/catalogo');
    await expect(page.getByRole('heading', { name: 'Celeste' })).toBeVisible();
    const historico = await page.evaluate(() => history.length);

    const campo = page.getByRole('searchbox', { name: 'Buscar por nome' });
    await campo.pressSequentially('hollow', { delay: 40 });

    await expect(page.getByRole('heading', { name: 'Hollow Knight' })).toBeVisible();
    await expect(campo).toBeFocused();
    await expect(page).toHaveURL(/\/catalogo\?nome=hollow$/);
    expect(pedidos.filter((p) => p.url().includes('nome='))).toHaveLength(1);
    expect(await page.evaluate(() => history.length)).toBe(historico);
  });

  test('filtro e página ficam na URL, e voltar restaura a busca anterior', async ({ page }) => {
    await simularCatalogo(page, (url) => {
      const numero = Number(url.searchParams.get('page'));
      return { ...pagina([produto(numero + 1, `Item ${numero}`)], [], 3, numero), totalElementos: 41 };
    });
    await page.goto('/catalogo?nome=hollow');

    await page.getByRole('button', { name: 'Jogos' }).click();
    await expect(page).toHaveURL(/tipo=JOGO/);
    await page.getByRole('button', { name: 'Próxima' }).click();
    await expect(page).toHaveURL(/pagina=2/);
    await expect(page.getByText('Página 2 de 3')).toBeVisible();

    await page.goBack();

    await expect(page).not.toHaveURL(/pagina=/);
    await expect(page.getByText('Página 1 de 3')).toBeVisible();
    await expect(page.getByRole('searchbox', { name: 'Buscar por nome' })).toHaveValue('hollow');
  });

  test('busca sem resultado local procura fora do catálogo e oferece ver na loja', async ({ page }) => {
    const pedidos = await simularCatalogo(page, (url) =>
      url.searchParams.get('incluirExterno') === 'true'
        ? pagina([], [{ gid: 'g1', nome: 'Hollow Edition', imagemUrl: null, melhorPreco: OFERTA }])
        : pagina([]),
    );
    await page.goto('/catalogo?nome=hollow');

    const externo = page.getByRole('region', { name: 'Fora do catálogo' });
    await expect(externo.getByRole('heading', { name: 'Hollow Edition' })).toBeVisible();
    await expect(externo.getByRole('link', { name: /Ver na loja/ })).toHaveAttribute('target', '_blank');
    expect(pedidos.filter((p) => p.url().includes('incluirExterno=true'))).toHaveLength(1);
  });

  test('nada no catálogo nem fora dele mostra o Umbra perdido e limpa os filtros', async ({ page }) => {
    await simularCatalogo(page, (url) => pagina(url.searchParams.has('nome') ? [] : [produto(2, 'Celeste')]));
    await page.goto('/catalogo?nome=xyzwqk&tipo=JOGO');

    await expect(page.getByRole('heading', { name: 'Nada com "xyzwqk"' })).toBeVisible();
    await expect(page.locator('umbra-mascote:visible')).toHaveCount(1);

    await page.getByRole('button', { name: 'Limpar filtros' }).click();

    await expect(page).toHaveURL(/\/catalogo$/);
    await expect(page.getByRole('heading', { name: 'Celeste' })).toBeVisible();
  });

  test('buscar um jogo da página não encontrada leva ao campo de busca', async ({ page }) => {
    await simularCatalogo(page, () => pagina([produto(2, 'Celeste')]));
    await page.goto('/rota-que-nao-existe');

    await page.getByRole('link', { name: 'Buscar um jogo' }).click();

    await expect(page.getByRole('searchbox', { name: 'Buscar por nome' })).toBeFocused();
  });
});
