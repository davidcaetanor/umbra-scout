export class ConfiguracaoInvalidaError extends Error {
  constructor(motivo: string) {
    super(`config.json inválido: ${motivo}`);
    this.name = 'ConfiguracaoInvalidaError';
  }
}
