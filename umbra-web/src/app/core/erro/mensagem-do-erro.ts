import { CasoDeErro } from './caso-de-erro';
import { casoDe } from './erro-da-api';

const TENTE_EM_INSTANTES = 'Tente de novo em instantes.';

export function mensagemDoCaso(caso: CasoDeErro): string | null {
  switch (caso.tipo) {
    case 'sessao-encerrada':
      return null;
    case 'rede':
      return 'Não conseguimos falar com o Umbra agora. Confira sua conexão e tente de novo.';
    case 'tempo-esgotado':
      return `O Umbra demorou demais para responder. ${TENTE_EM_INSTANTES}`;
    case 'credencial-recusada':
      return 'Não foi possível confirmar quem você é. Entre de novo.';
    case 'acesso-negado':
      return 'Você não tem acesso a este conteúdo.';
    case 'nao-encontrado':
      return 'Não encontramos o que você procurava.';
    case 'invalido':
      return 'Algum dado enviado não foi aceito. Confira e tente de novo.';
    case 'conflito':
    case 'regra':
      return caso.detalhe ?? 'Não foi possível concluir esta ação.';
    case 'indisponivel':
      return `O serviço está ocupado agora. ${tentarDeNovoEm(caso.tentarDeNovoEmSegundos)}`;
    case 'inesperado':
      return `Algo deu errado do nosso lado. ${TENTE_EM_INSTANTES}`;
  }
}

export function mensagemDoErro(erro: unknown): string | null {
  return mensagemDoCaso(casoDe(erro));
}

function tentarDeNovoEm(segundos: number | null): string {
  if (segundos === null) {
    return TENTE_EM_INSTANTES;
  }
  return `Tente de novo em ${segundos} ${segundos === 1 ? 'segundo' : 'segundos'}.`;
}
