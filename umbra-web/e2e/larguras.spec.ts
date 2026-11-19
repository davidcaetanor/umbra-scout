import { expect, test } from '@playwright/test';
import { comSessao } from './apoio/sessao';

const LARGURAS = [
  { nome: 'minima', largura: 360, altura: 780 },
  { nome: 'maxima', largura: 1920, altura: 1080 },
] as const;

const ROTAS = [
  { caminho: '/catalogo', titulo: 'Catálogo' },
  { caminho: '/monitoramentos', titulo: 'Meus monitoramentos' },
  { caminho: '/entrar', titulo: 'Entrar' },
  { caminho: '/cadastro', titulo: 'Criar conta' },
  { caminho: '/login/callback?erro=falha_no_provedor', titulo: 'Não deu para entrar' },
] as const;

for (const { nome, largura, altura } of LARGURAS) {
  test.describe(`RNF-08 em ${largura} px`, () => {
    test.use({ viewport: { width: largura, height: altura } });

    for (const rota of ROTAS) {
      test(`${rota.titulo} sem rolagem horizontal e sem conteúdo cortado`, async ({ page }, info) => {
        await comSessao(page);
        await page.goto(rota.caminho);
        await expect(page.getByRole('heading', { level: 1, name: rota.titulo })).toBeVisible();

        const medida = await page.evaluate(() => {
          const visivel = document.documentElement.clientWidth;
          const vazando = [...document.querySelectorAll('body *')]
            .filter((el) => el.getBoundingClientRect().right > visivel + 0.5)
            .map((el) => el.tagName.toLowerCase());
          return { visivel, total: document.documentElement.scrollWidth, vazando };
        });

        expect(medida.total, 'largura rolável igual à visível').toBe(medida.visivel);
        expect(medida.vazando, 'nenhum elemento passa da borda direita').toEqual([]);

        const mascotesVisiveis = await page.locator('umbra-mascote:visible, umbra-mascote-destaque:visible').count();
        expect(mascotesVisiveis, 'no máximo um Umbra por tela').toBeLessThanOrEqual(1);

        const captura = await page.screenshot({ fullPage: true });
        await info.attach(`${nome}${rota.caminho.split('?')[0].replaceAll('/', '-')}`, { body: captura, contentType: 'image/png' });
      });
    }
  });
}
