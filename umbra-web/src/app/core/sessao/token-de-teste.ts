export const AGORA = Date.UTC(2026, 10, 19, 12, 0, 0);

export function tokenQueExpiraEm(milissegundosAPartirDeAgora: number): string {
  const conteudo = { sub: '7', exp: Math.floor((AGORA + milissegundosAPartirDeAgora) / 1000) };
  return ['eyJhbGciOiJIUzI1NiJ9', base64Url(JSON.stringify(conteudo)), 'assinatura'].join('.');
}

function base64Url(texto: string): string {
  return btoa(texto).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
