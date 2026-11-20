import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { destinoInterno } from '../../../../core/navegacao/destino-interno';
import { FrasesEmLinhasPipe } from '../../../../shared/formatacao/frases-em-linhas.pipe';
import { AvisoComponent } from '../../components/aviso/aviso.component';
import { BotoesLoginExternoComponent } from '../../components/botoes-login-externo/botoes-login-externo.component';
import { MolduraEntradaComponent } from '../../components/moldura-entrada/moldura-entrada.component';
import { ProvedorExterno } from '../../data/auth.modelos';
import { AuthService } from '../../data/auth.service';

export const ROTA_DO_RETORNO = '/login/callback';

const MENSAGEM_POR_MOTIVO: Readonly<Record<string, string>> = {
  email_nao_verificado:
    'Seu email não está confirmado na conta escolhida. Confirme o email lá e tente de novo, ou entre com email e senha.',
  conta_inativa: 'Esta conta está desativada.',
  conta_vinculada_a_outra_identidade:
    'Este email já está ligado a outra conta desse provedor. Entre com a conta que você usou antes, ou com email e senha.',
  falha_no_provedor: 'O provedor não respondeu como esperado. Tente de novo em instantes.',
  entrada_cancelada: 'Você cancelou a entrada. Escolha uma forma de entrar quando quiser.',
  'codigo-invalido': 'O acesso expirou antes de ser confirmado. Tente entrar de novo.',
  falha: 'Não foi possível confirmar seu acesso agora. Tente de novo em instantes.',
};

const MOTIVO_DESCONHECIDO = 'falha_no_provedor';

export function mensagemDoMotivo(motivo: string): string {
  return MENSAGEM_POR_MOTIVO[motivo] ?? MENSAGEM_POR_MOTIVO[MOTIVO_DESCONHECIDO];
}

@Component({
  selector: 'umbra-retorno-page',
  imports: [FrasesEmLinhasPipe, RouterLink, AvisoComponent, BotoesLoginExternoComponent, MolduraEntradaComponent],
  templateUrl: './retorno.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetornoPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  readonly codigo = input<string | undefined>();
  readonly erro = input<string | undefined>();

  protected readonly provedores = this.auth.provedores;
  protected readonly saindoPara = signal<ProvedorExterno | null>(null);
  protected readonly falha = signal<string | null>(null);

  ngOnInit(): void {
    const codigo = this.codigo();
    const erro = this.erro();
    this.location.replaceState(ROTA_DO_RETORNO);
    if (erro !== undefined || !codigo) {
      this.falha.set(mensagemDoMotivo(erro ?? MOTIVO_DESCONHECIDO));
      return;
    }
    void this.trocar(codigo);
  }

  protected entrarCom(provedor: ProvedorExterno): void {
    this.saindoPara.set(provedor);
    this.auth.iniciarLoginExterno(provedor, destinoInterno(this.auth.retomarDestino()));
  }

  private async trocar(codigo: string): Promise<void> {
    const resultado = await this.auth.trocarCodigo(codigo);
    if (resultado === 'ok') {
      await this.router.navigateByUrl(destinoInterno(this.auth.retomarDestino()), { replaceUrl: true });
      return;
    }
    this.falha.set(mensagemDoMotivo(resultado));
  }
}
