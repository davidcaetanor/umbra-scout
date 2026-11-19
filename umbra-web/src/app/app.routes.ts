import { Routes } from '@angular/router';
import { autenticadoGuard } from './core/sessao';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalogo' },
  { path: 'catalogo', loadChildren: () => import('./features/produtos/produtos.routes').then((m) => m.PRODUTOS_ROUTES) },
  {
    path: 'monitoramentos',
    canActivate: [autenticadoGuard],
    loadChildren: () => import('./features/monitoramentos/monitoramentos.routes').then((m) => m.MONITORAMENTOS_ROUTES),
  },
  {
    path: 'conta',
    canActivate: [autenticadoGuard],
    loadChildren: () => import('./features/conta/conta.routes').then((m) => m.CONTA_ROUTES),
  },
  { path: 'entrar', loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES) },
  { path: 'cadastro', loadChildren: () => import('./features/auth/auth.routes').then((m) => m.CADASTRO_ROUTES) },
  { path: 'login/callback', loadChildren: () => import('./features/auth/auth.routes').then((m) => m.RETORNO_ROUTES) },
  { path: '**', redirectTo: 'catalogo' },
];
