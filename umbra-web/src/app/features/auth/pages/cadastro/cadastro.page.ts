import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ROTA_DE_ENTRADA, destinoInterno } from '../../../../core/navegacao/destino-interno';
import { AvisoComponent } from '../../components/aviso/aviso.component';
import { BotoesLoginExternoComponent } from '../../components/botoes-login-externo/botoes-login-externo.component';
import { CampoComponent } from '../../components/campo/campo.component';
import { MolduraEntradaComponent } from '../../components/moldura-entrada/moldura-entrada.component';
import { ProvedorExterno, ResultadoCadastro } from '../../data/auth.modelos';
import { AuthService } from '../../data/auth.service';
import { CONTA_CRIADA } from '../entrar/entrar.page';
import {
  MAXIMO_DO_EMAIL,
  MAXIMO_DO_NOME,
  MINIMO_DA_SENHA,
  focarPrimeiroInvalido,
  mensagemDoCampo,
  senhaDentroDoLimite,
} from '../formulario';

@Component({
  selector: 'umbra-cadastro-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    AvisoComponent,
    BotoesLoginExternoComponent,
    CampoComponent,
    MolduraEntradaComponent,
  ],
  templateUrl: './cadastro.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CadastroPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly raiz = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly returnUrl = input<string | undefined>();

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(MAXIMO_DO_NOME)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(MAXIMO_DO_EMAIL)]],
    senha: ['', [Validators.required, Validators.minLength(MINIMO_DA_SENHA), senhaDentroDoLimite]],
  });

  protected readonly provedores = this.auth.provedores;
  protected readonly minimoDaSenha = MINIMO_DA_SENHA;
  protected readonly enviando = signal(false);
  protected readonly saindoPara = signal<ProvedorExterno | null>(null);
  protected readonly falhou = signal(false);
  protected readonly destino = computed(() => destinoInterno(this.returnUrl()));
  protected readonly mensagem = mensagemDoCampo;

  protected async enviar(): Promise<void> {
    if (this.enviando()) {
      return;
    }
    if (this.formulario.invalid) {
      focarPrimeiroInvalido(this.formulario, this.raiz.nativeElement, 'cadastro');
      return;
    }
    this.enviando.set(true);
    this.falhou.set(false);
    const dados = this.formulario.getRawValue();
    const resultado = await this.auth.cadastrar(dados);
    await this.tratar(resultado, dados.email);
  }

  protected entrarCom(provedor: ProvedorExterno): void {
    this.saindoPara.set(provedor);
    this.auth.iniciarLoginExterno(provedor, this.destino());
  }

  private async tratar(resultado: ResultadoCadastro, email: string): Promise<void> {
    switch (resultado.tipo) {
      case 'ok':
        await this.router.navigateByUrl(this.destino());
        return;
      case 'criada-sem-entrar':
        await this.router.navigate([ROTA_DE_ENTRADA], {
          queryParams: { email, conta: CONTA_CRIADA, returnUrl: this.destino() },
        });
        return;
      case 'email-em-uso':
        this.recusarCampo('email', { emUso: true });
        break;
      case 'invalido':
        resultado.erros.forEach(({ campo, mensagem }) => this.recusarCampo(campo, { servidor: mensagem }));
        break;
      case 'falha':
        this.falhou.set(true);
        break;
    }
    this.enviando.set(false);
    if (resultado.tipo !== 'falha') {
      focarPrimeiroInvalido(this.formulario, this.raiz.nativeElement, 'cadastro');
    }
  }

  private recusarCampo(campo: 'nome' | 'email' | 'senha', erro: Record<string, unknown>): void {
    const controle = this.formulario.controls[campo];
    controle.setErrors(erro);
    controle.markAsTouched();
  }
}
