import { Routes } from '@angular/router';
import { PaginaNaoEncontradaPage } from './core/erro/pagina-nao-encontrada/pagina-nao-encontrada.page';
import { SEM_INDEXACAO } from './core/navegacao/indexacao-por-rota';
import { autenticadoGuard } from './core/sessao';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadChildren: () => import('./features/produtos/produtos.routes').then((m) => m.INICIO_ROUTES),
  },
  { path: 'catalogo', loadChildren: () => import('./features/produtos/produtos.routes').then((m) => m.PRODUTOS_ROUTES) },
  {
    path: 'monitoramentos',
    canActivate: [autenticadoGuard],
    data: SEM_INDEXACAO,
    loadChildren: () => import('./features/monitoramentos/monitoramentos.routes').then((m) => m.MONITORAMENTOS_ROUTES),
  },
  {
    path: 'conta',
    canActivate: [autenticadoGuard],
    data: SEM_INDEXACAO,
    loadChildren: () => import('./features/conta/conta.routes').then((m) => m.CONTA_ROUTES),
  },
  { path: 'entrar', data: SEM_INDEXACAO, loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES) },
  { path: 'cadastro', data: SEM_INDEXACAO, loadChildren: () => import('./features/auth/auth.routes').then((m) => m.CADASTRO_ROUTES) },
  { path: 'login/callback', data: SEM_INDEXACAO, loadChildren: () => import('./features/auth/auth.routes').then((m) => m.RETORNO_ROUTES) },
  { path: '**', component: PaginaNaoEncontradaPage, title: 'Página não encontrada' },
];
