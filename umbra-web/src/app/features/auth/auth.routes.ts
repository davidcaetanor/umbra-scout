import { Routes } from '@angular/router';
import { CadastroPage } from './pages/cadastro/cadastro.page';
import { EntrarPage } from './pages/entrar/entrar.page';
import { RetornoPage } from './pages/retorno/retorno.page';

export const AUTH_ROUTES: Routes = [{ path: '', component: EntrarPage, title: 'Entrar' }];

export const CADASTRO_ROUTES: Routes = [{ path: '', component: CadastroPage, title: 'Criar conta' }];

export const RETORNO_ROUTES: Routes = [{ path: '', component: RetornoPage, title: 'Entrando' }];
