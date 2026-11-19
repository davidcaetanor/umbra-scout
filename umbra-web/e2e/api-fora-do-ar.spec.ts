import { ConsoleMessage, expect, test } from '@playwright/test';
import { API, comSessao } from './apoio/sessao';

const ROTAS = [
  { caminho: '/catalogo', titulo: 'Catálogo' },
  { caminho: '/monitoramentos', titulo: 'Meus monitoramentos' },
  { caminho: '/conta', titulo: 'Minha conta' },
  { caminho: '/entrar', titulo: 'Entrar' },
  { caminho: '/cadastro', titulo: 'Criar conta' },
  { caminho: '/login/callback?codigo=abc', titulo: 'Não deu para entrar' },
  { caminho: '/rota-que-nao-existe', titulo: 'Página não encontrada' },
] as const;

const FALHA_DE_REDE_DO_NAVEGADOR = /net::ERR_|Failed to load resource/;

function ehErroDaAplicacao(mensagem: ConsoleMessage): boolean {
  return mensagem.type() === 'error' && !FALHA_DE_REDE_DO_NAVEGADOR.test(mensagem.text());
}

test.describe('RF-22 com a API fora do ar', () => {
  for (const rota of ROTAS) {
    test(`${rota.titulo} mostra a tela e nenhum erro da aplicação`, async ({ page }) => {
      const errosDaAplicacao: string[] = [];
      page.on('pageerror', (erro) => errosDaAplicacao.push(erro.message));
      page.on('console', (mensagem) => ehErroDaAplicacao(mensagem) && errosDaAplicacao.push(mensagem.text()));
      await comSessao(page);
      await page.route(`${API}/**`, (pedido) => pedido.abort('connectionrefused'));

      await page.goto(rota.caminho);

      await expect(page.getByRole('heading', { level: 1, name: rota.titulo })).toBeVisible();
      await expect(page.getByRole('navigation').first()).toBeVisible();
      expect(errosDaAplicacao).toEqual([]);
    });
  }

  test('Minha conta mostra a falha no bloco e se recupera ao tentar de novo', async ({ page }) => {
    await comSessao(page);
    let apiNoAr = false;
    await page.route(`${API}/usuarios/eu`, (pedido) =>
      apiNoAr
        ? pedido.fulfill({ json: { id: 7, email: 'ana@exemplo.com', nomeExibicao: 'Ana', cep: null, cidade: null, uf: null, criadoEm: '2026-11-01T00:00:00Z' } })
        : pedido.abort('connectionrefused'),
    );

    await page.goto('/conta');
    const alerta = page.getByRole('alert');
    await expect(alerta).toContainText('Não conseguimos falar com o Umbra agora.');

    apiNoAr = true;
    await alerta.getByRole('button', { name: 'Tentar de novo' }).click();

    await expect(page.getByRole('alert')).toHaveCount(0);
    await expect(page.getByText('ana@exemplo.com')).toBeVisible();
  });
});
