import { Routes } from '@angular/router';
import { CatalogoPage } from './pages/catalogo/catalogo.page';
import { InicioPage } from './pages/inicio/inicio.page';

export const PRODUTOS_ROUTES: Routes = [
  { path: '', component: CatalogoPage, title: 'Catálogo' },
  { path: ':id', loadComponent: () => import('./pages/detalhe/detalhe.page').then((m) => m.DetalhePage), title: 'Produto' },
];

export const INICIO_ROUTES: Routes = [{ path: '', component: InicioPage, title: 'Início' }];
