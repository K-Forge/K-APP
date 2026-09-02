import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { PageResponse } from '../../../core/http/page-response.model';
import type { UserListFilters, UserProfile } from './user.model';

/** Thin wrapper over /api/users - list/search, read one, and the activation toggle. */
@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly api = inject(ApiClientService);

  list(filters: UserListFilters): Observable<PageResponse<UserProfile>> {
    return this.api.get<PageResponse<UserProfile>>('/api/users', {
      page: filters.page,
      size: filters.size,
      role: filters.role,
      active: filters.active,
      q: filters.q,
    });
  }

  getById(userId: string): Observable<UserProfile> {
    return this.api.get<UserProfile>(`/api/users/${encodeURIComponent(userId)}`);
  }

  setStatus(userId: string, active: boolean): Observable<UserProfile> {
    return this.api.patch<UserProfile>(`/api/users/${encodeURIComponent(userId)}/status`, { active });
  }
}
