import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app';
import { criarAppConfig } from './app.config';
import { carregarConfiguracao } from './core/ambiente';

const MENSAGEM_DE_FALHA = 'Não foi possível iniciar o Umbra. Recarregue a página em alguns instantes.';

export async function iniciarAplicacao(): Promise<void> {
  try {
    const configuracao = await carregarConfiguracao();
    await bootstrapApplication(App, criarAppConfig(configuracao));
  } catch (erro) {
    console.error(erro);
    document.body.textContent = MENSAGEM_DE_FALHA;
  }
}
