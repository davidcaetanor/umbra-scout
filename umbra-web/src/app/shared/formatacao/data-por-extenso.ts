const FORMATO = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeZone: 'America/Sao_Paulo' });

export function dataPorExtenso(instante: string): string {
  const data = new Date(instante);
  return Number.isNaN(data.getTime()) ? '' : FORMATO.format(data);
}
