import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenStore } from './token.store';

/**
 * Attaches the bearer token to every outgoing request and reacts to a 401 the same way
 * everywhere: clear the session and return to login instead of leaving the UI in a half-signed-in
 * state showing stale data behind failed requests. The error still propagates after that, so the
 * screen that made the call (the login form included) can show its own message.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStore = inject(TokenStore);
  const router = inject(Router);

  const token = tokenStore.raw();
  const authedReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authedReq).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401) {
        tokenStore.clear();
        if (!router.url.startsWith('/login')) {
          router.navigateByUrl('/login');
        }
      }
      return throwError(() => err);
    }),
  );
};
