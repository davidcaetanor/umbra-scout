const SEGMENTOS_DO_JWT = 3;

export function expiracaoDoToken(token: string): number | null {
  const segmentos = token.split('.');
  if (segmentos.length !== SEGMENTOS_DO_JWT) {
    return null;
  }
  try {
    const conteudo: unknown = JSON.parse(decodificarBase64Url(segmentos[1]));
    if (typeof conteudo !== 'object' || conteudo === null) {
      return null;
    }
    const { exp } = conteudo as Record<string, unknown>;
    return typeof exp === 'number' && Number.isFinite(exp) ? exp * 1000 : null;
  } catch {
    return null;
  }
}

function decodificarBase64Url(valor: string): string {
  const base64 = valor.replace(/-/g, '+').replace(/_/g, '/');
  const completo = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  return new TextDecoder().decode(Uint8Array.from(atob(completo), (caractere) => caractere.charCodeAt(0)));
}
