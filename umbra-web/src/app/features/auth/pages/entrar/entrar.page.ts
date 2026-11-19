import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MOTIVO_SESSAO_EXPIRADA } from '../../../../core/http';
import { destinoInterno } from '../../../../core/navegacao/destino-interno';
import { FrasesEmLinhasPipe } from '../../../../shared/formatacao/frases-em-linhas.pipe';
import { AvisoComponent } from '../../components/aviso/aviso.component';
import { BotoesLoginExternoComponent } from '../../components/botoes-login-externo/botoes-login-externo.component';
import { CampoComponent } from '../../components/campo/campo.component';
import { MolduraEntradaComponent } from '../../components/moldura-entrada/moldura-entrada.component';
import { ProvedorExterno } from '../../data/auth.modelos';
import { AuthService } from '../../data/auth.service';
import { MAXIMO_DO_EMAIL, focarPrimeiroInvalido, mensagemDoCampo, senhaDentroDoLimite } from '../formulario';
import { destravarAoVoltar } from '../destravar-ao-voltar';

export const CONTA_CRIADA = 'criada';

type FalhaDaEntrada = 'credenciais' | 'falha';

@Component({
  selector: 'umbra-entrar-page',
  imports: [
    FrasesEmLinhasPipe,
    ReactiveFormsModule,
    RouterLink,
    AvisoComponent,
    BotoesLoginExternoComponent,
    CampoComponent,
    MolduraEntradaComponent,
  ],
  templateUrl: './entrar.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntrarPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly raiz = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly returnUrl = input<string | undefined>();
  readonly motivo = input<string | undefined>();
  readonly email = input<string | undefined>();
  readonly conta = input<string | undefined>();

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(MAXIMO_DO_EMAIL)]],
    senha: ['', [Validators.required, senhaDentroDoLimite]],
  });

  protected readonly provedores = this.auth.provedores;
  protected readonly enviando = signal(false);
  protected readonly saindoPara = signal<ProvedorExterno | null>(null);

  constructor() {
    destravarAoVoltar(this.saindoPara);
  }
  protected readonly falha = signal<FalhaDaEntrada | null>(null);
  protected readonly sessaoExpirada = computed(() => this.motivo() === MOTIVO_SESSAO_EXPIRADA);
  protected readonly contaCriada = computed(() => this.conta() === CONTA_CRIADA);
  protected readonly destino = computed(() => destinoInterno(this.returnUrl()));
  protected readonly mensagem = mensagemDoCampo;

  ngOnInit(): void {
    const email = this.email();
    if (email) {
      this.formulario.controls.email.setValue(email);
    }
  }

  protected async enviar(): Promise<void> {
    if (this.enviando()) {
      return;
    }
    if (this.formulario.invalid) {
      focarPrimeiroInvalido(this.formulario, this.raiz.nativeElement, 'entrar');
      return;
    }
    this.enviando.set(true);
    this.falha.set(null);
    const resultado = await this.auth.entrar(this.formulario.getRawValue());
    if (resultado === 'ok') {
      await this.router.navigateByUrl(this.destino());
      return;
    }
    this.enviando.set(false);
    this.falha.set(resultado);
  }

  protected entrarCom(provedor: ProvedorExterno): void {
    this.saindoPara.set(provedor);
    this.auth.iniciarLoginExterno(provedor, this.destino());
  }
}
