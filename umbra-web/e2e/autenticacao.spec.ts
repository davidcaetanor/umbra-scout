import { expect, test } from '@playwright/test';
import { API, CHAVE_DO_TOKEN, comSessao, responderUsuario, tokenQueExpiraEm } from './apoio/sessao';

const USUARIO_CRIADO = { id: 7 };
const TOKEN_RESPONSE = (token: string) => ({ token, tipo: 'Bearer', expiraEm: '2026-11-19T18:00:00Z' });

test.describe('T4.2, autenticação no frontend', () => {
  test('rota protegida sem sessão leva ao login e volta depois de entrar', async ({ page }) => {
    const token = tokenQueExpiraEm(6 * 3600);
    await responderUsuario(page);
    await page.route(`${API}/auth/login`, (rota) => rota.fulfill({ json: TOKEN_RESPONSE(token) }));

    await page.goto('/monitoramentos');
    await expect(page).toHaveURL(/\/entrar\?returnUrl=%2Fmonitoramentos$/);

    await page.getByLabel('Email').fill('ana@exemplo.com');
    await page.getByLabel('Senha', { exact: true }).fill('segredo123');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();

    await expect(page.getByRole('heading', { level: 1, name: 'Meus monitoramentos' })).toBeVisible();
    await expect(page.getByRole('button', { name: /Conta de Ana/ })).toBeVisible();
    expect(await page.evaluate((chave) => localStorage.getItem(chave), CHAVE_DO_TOKEN)).toBe(token);
  });

  test('login recusado mostra mensagem genérica e não sai da tela', async ({ page }) => {
    await page.route(`${API}/auth/login`, (rota) => rota.fulfill({ status: 401, json: { status: 401 } }));

    await page.goto('/entrar');
    await page.getByLabel('Email').fill('ana@exemplo.com');
    await page.getByLabel('Senha', { exact: true }).fill('errada');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();

    await expect(page.getByRole('alert')).toHaveText('Email ou senha incorretos.');
    await expect(page).toHaveURL(/\/entrar$/);
  });

  test('token vencido no armazenamento é descartado sem quebrar a aplicação', async ({ page }) => {
    const erros: string[] = [];
    page.on('pageerror', (erro) => erros.push(erro.message));
    await comSessao(page, tokenQueExpiraEm(30));

    await page.goto('/monitoramentos');

    await expect(page.getByRole('heading', { level: 1, name: 'Entrar' })).toBeVisible();
    expect(await page.evaluate((chave) => localStorage.getItem(chave), CHAVE_DO_TOKEN)).toBeNull();
    expect(erros).toEqual([]);
  });

  test('401 com sessão aberta leva ao login avisando que a sessão expirou', async ({ page }) => {
    await page.route(`${API}/usuarios/eu`, (rota) => rota.fulfill({ status: 401, json: { status: 401 } }));
    await page.addInitScript(
      ([chave, valor]) => window.localStorage.setItem(chave, valor),
      [CHAVE_DO_TOKEN, tokenQueExpiraEm(6 * 3600)],
    );

    await page.goto('/catalogo');

    await expect(page.getByRole('status').filter({ hasText: 'Sua sessão expirou' })).toBeVisible();
    await expect(page).toHaveURL(/\/entrar\?returnUrl=.*motivo=expirada/);
  });

  test('Sair encerra a sessão e volta ao catálogo', async ({ page }) => {
    await comSessao(page);

    await page.goto('/monitoramentos');
    await page.getByRole('button', { name: /Conta de Ana/ }).click();
    await page.getByRole('button', { name: 'Sair' }).click();

    await expect(page).toHaveURL(/\/catalogo$/);
    await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible();
    expect(await page.evaluate((chave) => localStorage.getItem(chave), CHAVE_DO_TOKEN)).toBeNull();
  });

  test('retorno do provedor troca o código, tira-o da barra e volta ao destino', async ({ page }) => {
    const token = tokenQueExpiraEm(6 * 3600);
    let trocas = 0;
    await responderUsuario(page);
    await page.route(`${API}/auth/oauth2/token`, (rota) => {
      trocas += 1;
      return rota.fulfill({ json: TOKEN_RESPONSE(token) });
    });
    await page.addInitScript(() => sessionStorage.setItem('umbra.login-externo.destino', '/monitoramentos'));

    await page.goto('/login/callback?codigo=abc123');

    await expect(page.getByRole('heading', { level: 1, name: 'Meus monitoramentos' })).toBeVisible();
    expect(trocas).toBe(1);
    await page.goBack();
    expect(page.url()).not.toContain('codigo=');
  });

  test('retorno com erro do provedor explica e oferece outra saída', async ({ page }) => {
    await page.goto('/login/callback?erro=email_nao_verificado');

    await expect(page.getByRole('heading', { level: 1, name: 'Não deu para entrar' })).toBeVisible();
    await expect(page.getByRole('alert')).toContainText('Seu email não está confirmado');
    await expect(page).toHaveURL(/\/login\/callback$/);
    await expect(page.getByRole('link', { name: 'Entrar com email e senha' })).toBeVisible();
  });

  test('cadastro cria a conta e entra direto', async ({ page }) => {
    const token = tokenQueExpiraEm(6 * 3600);
    await responderUsuario(page);
    await page.route(`${API}/auth/cadastro`, (rota) => rota.fulfill({ status: 201, json: USUARIO_CRIADO }));
    await page.route(`${API}/auth/login`, (rota) => rota.fulfill({ json: TOKEN_RESPONSE(token) }));

    await page.goto('/cadastro?returnUrl=%2Fmonitoramentos');
    await page.getByLabel('Nome').fill('Ana');
    await page.getByLabel('Email').fill('ana@exemplo.com');
    await page.getByLabel('Senha', { exact: true }).fill('segredo123');
    await page.getByRole('button', { name: 'Criar conta' }).click();

    await expect(page.getByRole('heading', { level: 1, name: 'Meus monitoramentos' })).toBeVisible();
  });

  test('o botão do provedor sai para a API com o destino guardado', async ({ page }) => {
    await page.route(`${API}/oauth2/authorization/discord`, (rota) => rota.fulfill({ body: 'provedor', contentType: 'text/plain' }));

    await page.goto('/entrar?returnUrl=%2Fmonitoramentos');
    await page.getByRole('button', { name: 'Entrar com Discord' }).click();

    await expect(page).toHaveURL(`${API}/oauth2/authorization/discord`);
  });
});
