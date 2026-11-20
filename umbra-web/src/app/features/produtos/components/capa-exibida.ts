export interface CapaExibida {
  readonly endereco: string;
  readonly alternativas: string;
  readonly reserva: string | null;
  readonly enquadramento: 'faixa' | 'foto';
}
