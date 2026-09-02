import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

// Grows one feature at a time as each is built (see AGENTS commit history) - a route is only
// added here once the component behind it exists, so the app always builds at every commit.
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'identity' },
      {
        path: 'identity',
        loadComponent: () => import('./features/identity/identity.page').then((m) => m.IdentityPage),
      },
      {
        path: 'console',
        loadComponent: () => import('./features/console/console.page').then((m) => m.ConsolePage),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
