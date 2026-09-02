import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { TokenStore } from './token.store';

/** Keeps every screen except /login behind a valid, unexpired token. */
export const authGuard: CanActivateFn = (_route, state) => {
  const tokenStore = inject(TokenStore);
  const router = inject(Router);

  if (tokenStore.isAuthenticated() && !tokenStore.isExpired()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
