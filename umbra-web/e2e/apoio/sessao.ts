import { Page } from '@playwright/test';

export const API = 'http://localhost:8080';
export const CHAVE_DO_TOKEN = 'umbra.sessao.token';
export const USUARIO = {
  id: 7,
  email: 'ana@exemplo.com',
  nomeExibicao: 'Ana',
  cep: null,
  cidade: null,
  uf: null,
  criadoEm: '2026-11-01T00:00:00Z',
};

export function tokenQueExpiraEm(segundos: number): string {
  const conteudo = Buffer.from(JSON.stringify({ sub: '7', exp: Math.floor(Date.now() / 1000) + segundos })).toString('base64url');
  return `eyJhbGciOiJIUzI1NiJ9.${conteudo}.assinatura`;
}

export async function responderUsuario(page: Page): Promise<void> {
  await page.route(`${API}/usuarios/eu`, (rota) => rota.fulfill({ json: USUARIO }));
}

export async function comSessao(page: Page, token = tokenQueExpiraEm(6 * 3600)): Promise<string> {
  await responderUsuario(page);
  await page.addInitScript(([chave, valor]) => window.localStorage.setItem(chave, valor), [CHAVE_DO_TOKEN, token]);
  return token;
}
