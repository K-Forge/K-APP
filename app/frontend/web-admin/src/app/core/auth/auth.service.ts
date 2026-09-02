import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { ApiConfigService } from '../config/api-config.service';
import { parseApiError } from '../http/api-error.util';
import { AppHttpError } from '../http/api-http-error';
import type { TokenResponse } from './auth.model';
import { TokenStore } from './token.store';

/**
 * Deliberately bypasses ApiClientService: login is the one call the app makes with no token to
 * attach, and it is also where a bad base URL first becomes visible, so its errors go through
 * the same AppHttpError/ApiError path as everything else rather than a special case.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApiConfigService);
  private readonly tokenStore = inject(TokenStore);
  private readonly router = inject(Router);

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.config.baseUrl()}/auth/login`, { email, password }).pipe(
      tap((response) => this.tokenStore.set(response.accessToken)),
      catchError((err) => throwError(() => new AppHttpError(parseApiError(err)))),
    );
  }

  logout(): void {
    this.tokenStore.clear();
    this.router.navigateByUrl('/login');
  }
}
