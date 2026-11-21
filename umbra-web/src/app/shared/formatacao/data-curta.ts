const FUSO = 'America/Sao_Paulo';
const DIA_E_MES = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: FUSO });
const DIA_MES_E_ANO = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: FUSO });
const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', timeZone: FUSO });

export function diaEMes(instante: number): string {
  return DIA_E_MES.format(instante);
}

export function diaMesEAno(instante: number): string {
  return DIA_MES_E_ANO.format(instante);
}

export function hora(instante: number): string {
  return HORA.format(instante);
}
