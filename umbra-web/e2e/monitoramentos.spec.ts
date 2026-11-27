import { Page, expect, test } from '@playwright/test';
import { API, comSessao } from './apoio/sessao';

const LARGURAS = [
  { nome: 'minima', largura: 360, altura: 780 },
  { nome: 'maxima', largura: 1920, altura: 1080 },
] as const;

function oferta(loja: string, valorCentavos: number) {
  return {
    coletadoEm: '2026-11-24T10:00:00Z',
    verificadoEm: '2026-11-24T10:00:00Z',
    descontoPct: null,
    expiraEm: null,
    gratuito: false,
    loja,
    situacao: 'vigente',
    url: `https://loja.exemplo/${loja}`,
    valorCentavos,
    valorOriginalCentavos: null,
  };
}

function monitoramento(id: number, nome: string, alvo: number, preco: number, parcial: Record<string, unknown> = {}) {
  return {
    id,
    produtoId: id * 10,
    nome,
    imagemUrl: null,
    precoAlvoCentavos: alvo,
    ativo: true,
    criadoEm: '2026-11-20T10:00:00Z',
    melhorPreco: oferta('Steam', preco),
    distanciaDoAlvoCentavos: preco - alvo,
    alvoAtingido: preco <= alvo,
    aparentementeDescontinuado: false,
    ultimoAvisoEm: preco <= alvo ? '2026-11-27T18:30:00Z' : null,
    precoAvisadoCentavos: preco <= alvo ? preco : null,
    ...parcial,
  };
}

const LISTA = [
  monitoramento(1, 'Hollow Knight: Silksong', 8000, 6990),
  monitoramento(2, 'Black Myth: Wukong', 15000, 17249),
  monitoramento(3, 'Placa de vídeo RTX 4070 Super 12GB com nome comprido para testar a quebra', 380000, 419900),
  monitoramento(4, 'Elden Ring', 12000, 19950, { ativo: false }),
];

async function simular(page: Page, itens: unknown[]): Promise<{ removidos: number[] }> {
  const estado = { itens: [...itens] as { id: number; ativo: boolean }[], removidos: [] as number[] };
  await page.route(`${API}/monitoramentos?**`, (rota) =>
    rota.fulfill({
      json: { conteudo: estado.itens, pagina: 0, tamanho: 20, totalElementos: estado.itens.length, totalPaginas: 1 },
    }),
  );
  await page.route(new RegExp(`${API}/monitoramentos/\\d+$`), async (rota) => {
    const id = Number(rota.request().url().split('/').pop());
    if (rota.request().method() === 'DELETE') {
      estado.removidos.push(id);
      estado.itens = estado.itens.filter((item) => item.id !== id);
      return rota.fulfill({ status: 204 });
    }
    const corpo = rota.request().postDataJSON() as { ativo?: boolean; precoAlvoCentavos?: number };
    const item = { ...estado.itens.find((atual) => atual.id === id)!, ...corpo };
    estado.itens = estado.itens.map((atual) => (atual.id === id ? item : atual));
    return rota.fulfill({ json: item });
  });
  return estado;
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

const SITUACOES = [
  { nome: 'com-novidade', itens: LISTA, fala: 'Achei!' },
  { nome: 'vazia', itens: [], fala: 'Escolha um produto no catálogo' },
  { nome: 'todos-pausados', itens: LISTA.map((item) => ({ ...item, ativo: false, alvoAtingido: false })), fala: 'Tudo pausado' },
] as const;

for (const { nome, largura, altura } of LARGURAS) {
  test.describe(`Meus monitoramentos em ${largura} px`, () => {
    test.use({ viewport: { width: largura, height: altura } });

    for (const situacao of SITUACOES) {
      test(`${situacao.nome} sem rolagem horizontal e com um Umbra só`, async ({ page }, info) => {
        await comSessao(page);
        await simular(page, situacao.itens);
        await page.goto('/monitoramentos');

        await expect(page.getByRole('heading', { level: 1, name: 'Meus monitoramentos' })).toBeVisible();
        await expect(page.locator('umbra-balao-fala:visible').first()).toContainText(situacao.fala);

        const medida = await medirLargura(page);
        expect(medida.total, 'largura rolável igual à visível').toBe(medida.visivel);
        expect(medida.vazando, 'nenhum elemento passa da borda direita').toEqual([]);
        const mascotes = await page.locator('umbra-mascote:visible, umbra-mascote-destaque:visible').count();
        expect(mascotes, 'no máximo um Umbra por tela').toBeLessThanOrEqual(1);

        await info.attach(`${nome}-${situacao.nome}`, { body: await page.screenshot({ fullPage: true }), contentType: 'image/png' });
      });
    }

    test('remoção pelo teclado confirma, anuncia e leva o foco ao item seguinte', async ({ page }, info) => {
      await comSessao(page);
      const estado = await simular(page, LISTA);
      await page.goto('/monitoramentos');

      const remover = page.getByRole('button', { name: 'Remover Black Myth: Wukong' });
      await remover.focus();
      await page.keyboard.press('Enter');

      const dialogo = page.getByRole('alertdialog', { name: 'Remover Black Myth: Wukong?' });
      await expect(dialogo).toBeVisible();
      await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused();
      await info.attach(`${nome}-confirmar-remocao`, { body: await page.screenshot(), contentType: 'image/png' });

      await page.keyboard.press('Escape');
      await expect(dialogo).toBeHidden();
      await expect(remover).toBeFocused();
      expect(estado.removidos).toEqual([]);

      await page.keyboard.press('Enter');
      await dialogo.getByRole('button', { name: 'Remover' }).click();

      await expect(dialogo).toBeHidden();
      await expect(page.getByRole('heading', { level: 2, name: /Placa de vídeo RTX/ })).toBeFocused();
      await expect(page.getByRole('status').filter({ hasText: 'Black Myth: Wukong removido' })).toHaveCount(1);
      expect(estado.removidos).toEqual([2]);
    });

    test('alterar o alvo pelo teclado sem sair da tela', async ({ page }, info) => {
      await comSessao(page);
      await simular(page, LISTA);
      await page.goto('/monitoramentos');

      const alterar = page.getByRole('button', { name: 'Alterar alvo de Black Myth: Wukong' });
      await alterar.focus();
      await page.keyboard.press('Enter');

      const campo = page.getByLabel('Novo alvo');
      await expect(campo).toBeFocused();
      await expect(campo).toHaveValue('150,00');
      await info.attach(`${nome}-alterar-alvo`, { body: await page.screenshot(), contentType: 'image/png' });

      await campo.fill('120,50');
      await page.keyboard.press('Enter');

      await expect(campo).toBeHidden();
      await expect(page).toHaveURL(/\/monitoramentos$/);
      await expect(alterar).toBeFocused();
      await expect(page.getByRole('listitem').filter({ hasText: 'Black Myth: Wukong' })).toContainText('R$ 120,50');
    });

    test('pausar troca o rótulo e mantém o foco no botão', async ({ page }) => {
      await comSessao(page);
      await simular(page, LISTA);
      await page.goto('/monitoramentos');

      const pausar = page.getByRole('button', { name: 'Pausar Black Myth: Wukong' });
      await pausar.click();

      const reativar = page.getByRole('button', { name: 'Reativar Black Myth: Wukong' });
      await expect(reativar).toBeFocused();
      await expect(page.getByText('2 ativos · 2 pausados')).toBeVisible();
    });
  });
}
