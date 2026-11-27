import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { expect, test } from '@playwright/test';
import { API } from './apoio/sessao';

const RAIZ_DO_FRONT = resolve(__dirname, '..');
const CLS_MAXIMO_NA_TROCA_DE_FONTE = 0.005;
const DIFERENCA_MAXIMA_DE_LARGURA_DO_TEXTO = 0.04;
const LARGURAS_DA_TELA = [360, 1280];
const UM_JOGO = {
  conteudo: [
    {
      id: 1,
      nome: 'Hollow Knight',
      tipo: 'JOGO',
      categoria: null,
      imagemUrl: null,
      melhorPreco: {
        coletadoEm: '2026-11-20T10:00:00Z',
        descontoPct: 50,
        expiraEm: null,
        gratuito: false,
        loja: 'Steam',
        situacao: 'vigente',
        url: 'https://store.steampowered.com/app/367520',
        valorCentavos: 2349,
        valorOriginalCentavos: 4699,
      },
    },
  ],
  externos: [],
  pagina: 0,
  tamanho: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

function enderecos(texto: string, padrao: RegExp): string[] {
  return [...texto.matchAll(padrao)].map(([, endereco]) => endereco);
}

test.describe('T4.6d, fonte sem salto', () => {
  test('cada fonte declarada e pré-carregada existe em public com caminho fixo', () => {
    const declaradas = enderecos(readFileSync(resolve(RAIZ_DO_FRONT, 'src/styles/fontes.css'), 'utf8'), /url\('([^']+)'\)/g);
    const preCarregadas = enderecos(readFileSync(resolve(RAIZ_DO_FRONT, 'src/index.html'), 'utf8'), /rel="preload" href="([^"]+)"/g);

    expect(declaradas.length).toBeGreaterThan(0);
    expect(preCarregadas).toHaveLength(2);
    for (const endereco of [...declaradas, ...preCarregadas]) {
      expect(endereco).toMatch(/^\/fontes\/v\d+\/[a-z0-9-]+\.woff2$/);
      expect(existsSync(resolve(RAIZ_DO_FRONT, 'public', `.${endereco}`))).toBe(true);
    }
    expect(declaradas).toEqual(expect.arrayContaining(preCarregadas));
  });

  for (const caminho of ['/', '/catalogo']) {
    for (const largura of LARGURAS_DA_TELA) {
      test(`troca de fonte em ${caminho} a ${largura} px, depois da primeira pintura, sem salto e com a reserva da mesma largura`, async ({ page }) => {
        await page.setViewportSize({ width: largura, height: 800 });
        await page.route(`${API}/**`, (pedido) => pedido.fulfill({ json: UM_JOGO }));
        let liberarFontes = () => {};
        const fontesLiberadas = new Promise<void>((pronto) => (liberarFontes = pronto));
        await page.route('**/fontes/v1/*.woff2', async (pedido) => {
          await fontesLiberadas;
          await pedido.continue();
        });
        await page.addInitScript(() => {
          const janela = window as unknown as { deslocamentos: number[] };
          janela.deslocamentos = [];
          new PerformanceObserver((lista) => {
            for (const entrada of lista.getEntries() as unknown as { value: number; hadRecentInput: boolean }[]) {
              if (!entrada.hadRecentInput) {
                janela.deslocamentos.push(entrada.value);
              }
            }
          }).observe({ type: 'layout-shift', buffered: true });
        });
        const larguraDoTexto = (seletor: string) =>
          page.locator(seletor).first().evaluate((elemento) => {
            const trecho = document.createRange();
            trecho.selectNodeContents(elemento);
            return trecho.getBoundingClientRect().width;
          });

        await page.goto(caminho, { waitUntil: 'commit' });
        await page.getByRole('heading', { level: 1 }).first().waitFor();
        await page.locator('main p').first().waitFor();
        const tituloNaReserva = await larguraDoTexto('h1');
        const textoNaReserva = await larguraDoTexto('main p');
        liberarFontes();
        await expect.poll(() => page.evaluate(() => document.fonts.check("16px 'IBM Plex Sans'") && document.fonts.check("32px 'Fraunces Variable'"))).toBe(true);
        await page.evaluate(() => document.fonts.ready);

        const deslocamentos = await page.evaluate(() => (window as unknown as { deslocamentos: number[] }).deslocamentos);
        expect(deslocamentos.reduce((soma, valor) => soma + valor, 0)).toBeLessThan(CLS_MAXIMO_NA_TROCA_DE_FONTE);
        expect(Math.abs((await larguraDoTexto('h1')) / tituloNaReserva - 1)).toBeLessThan(DIFERENCA_MAXIMA_DE_LARGURA_DO_TEXTO);
        expect(Math.abs((await larguraDoTexto('main p')) / textoNaReserva - 1)).toBeLessThan(DIFERENCA_MAXIMA_DE_LARGURA_DO_TEXTO);
      });
    }
  }
});
