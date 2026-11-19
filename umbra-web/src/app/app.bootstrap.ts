import { ApplicationRef } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app';
import { criarAppConfig } from './app.config';
import { ConfiguracaoAmbiente, carregarConfiguracao } from './core/ambiente';

export const MENSAGEM_DE_FALHA_NA_PARTIDA = 'Não foi possível iniciar o Umbra. Recarregue a página em alguns instantes.';

export interface DependenciasDaPartida {
  readonly carregar: () => Promise<ConfiguracaoAmbiente>;
  readonly iniciar: (configuracao: ConfiguracaoAmbiente) => Promise<ApplicationRef>;
  readonly corpo: HTMLElement;
}

const dependenciasPadrao: DependenciasDaPartida = {
  carregar: () => carregarConfiguracao(),
  iniciar: (configuracao) => bootstrapApplication(App, criarAppConfig(configuracao)),
  get corpo() {
    return document.body;
  },
};

export async function iniciarAplicacao(dependencias: DependenciasDaPartida = dependenciasPadrao): Promise<void> {
  try {
    await dependencias.iniciar(await dependencias.carregar());
  } catch (erro) {
    console.error(erro);
    dependencias.corpo.textContent = MENSAGEM_DE_FALHA_NA_PARTIDA;
  }
}
