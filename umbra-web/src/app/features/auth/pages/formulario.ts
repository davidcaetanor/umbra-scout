import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

export const LIMITE_DA_SENHA_EM_BYTES = 72;
export const MINIMO_DA_SENHA = 8;
export const MAXIMO_DO_EMAIL = 320;
export const MAXIMO_DO_NOME = 60;

export const senhaDentroDoLimite: ValidatorFn = (controle: AbstractControl<string>): ValidationErrors | null =>
  new TextEncoder().encode(controle.value ?? '').length > LIMITE_DA_SENHA_EM_BYTES ? { senhaLonga: true } : null;

const MENSAGENS: Readonly<Record<string, (detalhe: unknown) => string>> = {
  required: () => 'Preencha este campo.',
  email: () => 'Informe um email válido, como voce@exemplo.com.',
  minlength: (detalhe) => `Use pelo menos ${(detalhe as { requiredLength: number }).requiredLength} caracteres.`,
  maxlength: (detalhe) => `Use no máximo ${(detalhe as { requiredLength: number }).requiredLength} caracteres.`,
  senhaLonga: () => 'Senha longa demais. Use uma mais curta.',
  emUso: () => 'Este email já tem conta. Entre com ele ou use outro.',
  servidor: (detalhe) => `Este campo ${String(detalhe)}.`,
};

export function mensagemDoCampo(controle: AbstractControl): string | null {
  if (!controle.touched || controle.errors === null) {
    return null;
  }
  const [chave, detalhe] = Object.entries(controle.errors)[0];
  return MENSAGENS[chave]?.(detalhe) ?? 'Verifique este campo.';
}

export function focarPrimeiroInvalido(formulario: FormGroup, raiz: HTMLElement, prefixo: string): void {
  formulario.markAllAsTouched();
  const primeiro = Object.keys(formulario.controls).find((nome) => formulario.controls[nome].invalid);
  if (primeiro !== undefined) {
    raiz.querySelector<HTMLElement>(`#${prefixo}-${primeiro}`)?.focus();
  }
}
